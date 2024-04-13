#include <iostream>
#include <string>
#include <cstdlib>
#include <cstdio>
#include <csignal>
#include <cstring>

#include "rdkafkacpp.h"


std::string brokers, topic;

static volatile sig_atomic_t run = 1;

static void sigterm(int sig) {
    run = 0;
}

class DeliveryReportCallback : public RdKafka::DeliveryReportCb {
public:
    DeliveryReportCallback() {}
    virtual ~DeliveryReportCallback() {}
    void dr_cb(RdKafka::Message& message) {
        if (message.err())
            std::cerr << "% Message delivery failed: " << message.errstr()
            << std::endl;
        else
            std::cerr << "% Message delivered to topic " << message.topic_name()
            << " [" << message.partition() << "] at offset "
            << message.offset() << std::endl;
    }
};

void produce(RdKafka::Producer* producer) {

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


int main() {
    std::string errstr;
    RdKafka::Conf* conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);

    brokers = "kafka:29092";
    //bootstrap server. broker 여러개면 "*,*.."



    std::cerr << "Usage: " << brokers << " <brokers> <topic>\n";



    /* Set bootstrap broker(s) as a comma-separated list of
     * host:port (default port=9092)*/
    if (conf->set("bootstrap.servers", brokers, errstr) != RdKafka::Conf::CONF_OK) {
        std::cerr << "conf_ok error :"<<errstr << std::endl;
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

    delete conf;

    std::cin >> topic;

    produce(producer);

    delete producer;


    return 0;
}
