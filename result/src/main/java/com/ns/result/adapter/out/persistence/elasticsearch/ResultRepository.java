package com.ns.result.adapter.out.persistence.elasticsearch;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;

public interface ResultRepository extends ReactiveElasticsearchRepository<Result, String> {
    @Query("""
                {
                  "bool": {
                    "should": [
                      {
                        "nested": {
                          "path": "blueTeams",
                          "query": {
                            "match": {
                              "blueTeams.user_name": "?0"
                            }
                          }
                        }
                      },
                      {
                        "nested": {
                          "path": "redTeams",
                          "query": {
                            "match": {
                              "redTeams.user_name": "?0"
                            }
                          }
                        }
                      }
                    ]
                  }
                },
                "from": ?1, "size": ?2"
            """)
    Flux<Result> searchByUserName(String userName, int offset, int size);


    @Query("""
                {
                  "bool": {
                    "should": [
                      {
                        "nested": {
                          "path": "blueTeams",
                          "query": {
                            "term": {
                              "blueTeams.membershipId": ?0
                            }
                          }
                        }
                      },
                      {
                        "nested": {
                          "path": "redTeams",
                          "query": {
                            "term": {
                              "redTeams.membershipId": ?0
                            }
                          }
                        }
                      }
                    ]
                  }
                },
                "from": ?1, "size": ?2"
            """)
    Flux<Result> searchByMembershipId(Long membershipId, int offset, int size);


    @Query("""
          {
            "bool": {
              "must": [
                { "term": { "spaceId.keyword": "?0" } }
              ]
            }
          }
        """)
    Flux<Result> searchBySpaceId(String spaceId);

}

