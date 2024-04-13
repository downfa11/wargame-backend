#include <iostream>
#include <string>
#include <cstdlib>
#include <cstdio>
#include <csignal>
#include <cstring>

#include "rdkafkacpp.h"


std::string brokers, topic, topic_str;
int32_t partition, start_offset;

static volatile sig_atomic_t run = 1;

static void sigterm(int sig) {
    run = 0;
}


class DeliveryReportCallback : public RdKafka::DeliveryReportCb {
public:
    void dr_cb(RdKafka::Message& message) {
        std::string status_name;
        switch (message.status()) {
        case RdKafka::Message::MSG_STATUS_NOT_PERSISTED:
            status_name = "NotPersisted";
            break;
        case RdKafka::Message::MSG_STATUS_POSSIBLY_PERSISTED:
            status_name = "PossiblyPersisted";
            break;
        case RdKafka::Message::MSG_STATUS_PERSISTED:
            status_name = "Persisted";
            break;
        default:
            status_name = "Unknown?";
            break;
        }
        std::cout << "Message delivery for (" << message.len()
            << " bytes): " << status_name << ": " << message.errstr()
            << std::endl;
        if (message.key())
            std::cout << "Key: " << *(message.key()) << ";" << std::endl;
    }
};

void consume_message(RdKafka::Message* message, void* opaque) {
    const RdKafka::Headers* headers;

    switch (message->err()) {
    case RdKafka::ERR__TIMED_OUT:
        break;

    case RdKafka::ERR_NO_ERROR:
        /* Real message */
        std::cout << "Read msg at offset " << message->offset() << std::endl;
        if (message->key()) {
            std::cout << "Key: " << *message->key() << std::endl;
        }
        headers = message->headers();
        if (headers) {
            std::vector<RdKafka::Headers::Header> hdrs = headers->get_all();
            for (size_t i = 0; i < hdrs.size(); i++) {
                const RdKafka::Headers::Header hdr = hdrs[i];

                if (hdr.value() != NULL)
                    printf(" Header: %s = \"%.*s\"\n", hdr.key().c_str(),
                        (int)hdr.value_size(), (const char*)hdr.value());
                else
                    printf(" Header:  %s = NULL\n", hdr.key().c_str());
            }
        }
        printf("%.*s\n", static_cast<int>(message->len()),
            static_cast<const char*>(message->payload()));
        break;

    case RdKafka::ERR__PARTITION_EOF: //파티션의 마지막 메시지를 받으면 종료합니다.
        /* Last message */
        run = 0;
        break;

    case RdKafka::ERR__UNKNOWN_TOPIC:
    case RdKafka::ERR__UNKNOWN_PARTITION:
        std::cerr << "Consume failed: " << message->errstr() << std::endl;
        run = 0;
        break;

    default:
        /* Errors */
        std::cerr << "Consume failed: " << message->errstr() << std::endl;
        run = 0;
    }
}

class ConsumeCallback : public RdKafka::ConsumeCb {
public:
    void consume_cb(RdKafka::Message& msg, void* opaque) {
        consume_message(&msg, opaque);
    }
};

void ProduceMessage(RdKafka::Producer* producer) {

    std::cout << "Produce message." << std::endl;

    for (std::string line; run && std::getline(std::cin, line);) {
        if (line.empty()) {
            producer->poll(0);
            continue;
        }

    retry:
        RdKafka::ErrorCode err = producer->produce(
            topic, //topic
            RdKafka::Topic::PARTITION_UA, //partition. Unassigned
            RdKafka::Producer::RK_MSG_COPY, // message copy
            const_cast<char*>(line.c_str()),
            line.size(), // message size
            NULL, 0, // key, key size
            0, // timestamp(default=0, current)
            NULL, // message header
            NULL); // send report callback

        if (err != RdKafka::ERR_NO_ERROR) {
            std::cerr << "% Failed to produce to topic " << topic << ": "
                << RdKafka::err2str(err) << std::endl;

            if (err == RdKafka::ERR__QUEUE_FULL) {
                // Queue limited by configuration property (queue.buffering.max.messages, queue.buffering.max.kbytes)
                producer->poll(1000 /*block for max 1000ms*/);
                goto retry;
            }

        }
        else {
            std::cerr << "% Enqueued message (" << line.size() << " bytes) "
                << "for topic " << topic << std::endl;
        }

        producer->poll(0);
    }

    /* Wait for final messages to be delivered or fail.
     * flush() : 모든 메세지 전달될때까지 대기하는 poll()의 추상화 */
    std::cerr << "% Flushing final messages..." << std::endl;
    producer->flush(10 * 1000 /* wait for max 10 seconds */);

    if (producer->outq_len() > 0)
        std::cerr << "% " << producer->outq_len()
        << " message(s) were not delivered" << std::endl;
}

void ConsumeMessage(RdKafka::Topic* topic, RdKafka::Consumer* consumer) {

    int use_ccb = 0;

    ConsumeCallback consumeCallback;

    while (run) {
        if (use_ccb) {
            consumer->consume_callback(topic, partition, 1000, &consumeCallback,
                &use_ccb);
        }
        else {
            RdKafka::Message* msg = consumer->consume(topic, partition, 1000);
            consume_message(msg, NULL);
            delete msg;
        }
        consumer->poll(0);
    }

    consumer->stop(topic, partition);

    consumer->poll(1000);

    delete topic;
}

int main() {

    brokers = "localhost:9092";
    //bootstrap server. broker 여러개면 "*,*.."

    std::cin >> topic;

    std::cerr << "Usage: " << brokers << " <brokers> <topic>\n";

    std::string errstr;

    RdKafka::Conf* conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    RdKafka::Conf* tconf = RdKafka::Conf::create(RdKafka::Conf::CONF_TOPIC);

    partition = RdKafka::Topic::PARTITION_UA;
    start_offset = RdKafka::Topic::OFFSET_BEGINNING;

    /* Set bootstrap broker(s) as a comma-separated list of
     * host:port (default port=9092)*/
    if (conf->set("bootstrap.servers", brokers, errstr) !=
        RdKafka::Conf::CONF_OK) {
        std::cerr << errstr << std::endl;
        exit(1);
    }


    signal(SIGTERM, sigterm);


    DeliveryReportCallback ex_dr_cb;

    if (conf->set("dr_cb", &ex_dr_cb, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << errstr << std::endl;
        exit(1);
    }



    RdKafka::Producer* producer = RdKafka::Producer::create(conf, errstr);
    if (!producer) {
        std::cerr << "Failed to create producer: " << errstr << std::endl;
        exit(1);
    }


    ProduceMessage(producer);

    delete producer;



    RdKafka::Consumer* consumer = RdKafka::Consumer::create(conf, errstr);
    if (!consumer) {
        std::cerr << "Failed to create consumer: " << errstr << std::endl;
        exit(1);
    }
    std::cout << "% Created consumer " << consumer->name() << std::endl;

    //topic 설정
    RdKafka::Topic* topic =
        RdKafka::Topic::create(consumer, topic_str, tconf, errstr);
    if (!topic) {
        std::cerr << "Failed to create topic: " << errstr << std::endl;
        exit(1);
    }

    //지정된 partition의 offset에서부터 consume을 시작합니다.
    RdKafka::ErrorCode resp = consumer->start(topic, partition, start_offset);
    if (resp != RdKafka::ERR_NO_ERROR) {
        std::cerr << "Failed to start consumer: " << RdKafka::err2str(resp)
            << std::endl;
        exit(1);
    }
    ConsumeMessage(topic, consumer);
    delete consumer;

    delete conf;
    return 0;
}