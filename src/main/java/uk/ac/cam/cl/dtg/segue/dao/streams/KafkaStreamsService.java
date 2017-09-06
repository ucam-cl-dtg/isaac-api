/**
 * Copyright 2017 Dan Underwood
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.dao.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.TopologyBuilderException;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.KafkaStatisticsManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.streams.customProcessors.ThresholdAchievedProcessor;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/** Kafka streams processing service for deriving data streams from Isaac logged events
 *
 *  @author Dan Underwood
 */
public class KafkaStreamsService {

    private KafkaStreams streams;
    private PropertiesLoader globalProperties;
    private final PostgresSqlDb database;
    private final IContentManager contentManager;
    private final String contentIndex;
    private final KafkaTopicManager topicManager;
    private ThresholdAchievedProcessor achievementProcessor;

    private static final Logger log = LoggerFactory.getLogger(KafkaStatisticsManager.class);

    final static Serializer<JsonNode> jsonSerializer = new JsonSerializer();
    final static Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
    final static Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    final static Serde<String> stringSerde = Serdes.String();



    /** Returns single instance of streams service to dependants.
     *
     * @return streams
     *          - the single streams instance
     */
    public KafkaStreams getStream() {
        return streams;
    }


    /**
     *
     * @param globalProperties
     * @param database
     * @param contentManager
     * @param contentIndex
     */
    @Inject
    public KafkaStreamsService(final PropertiesLoader globalProperties,
                               final PostgresSqlDb database,
                               final IContentManager contentManager,
                               @Named(CONTENT_INDEX) final String contentIndex,
                               KafkaTopicManager topicManager)  {

        this.globalProperties = globalProperties;
        this.database = database;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.topicManager = topicManager;

        KStreamBuilder builder = new KStreamBuilder();
        Properties streamsConfiguration = new Properties();

        // kafka streaming config variables
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_APPNAME"));
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));
        streamsConfiguration.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.METADATA_MAX_AGE_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);
        streamsConfiguration.put(StreamsConfig.producerPrefix(ProducerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);

        topicManager.ensureTopicExists("topic_logged_events");
        topicManager.ensureTopicExists("topic_anonymous_logged_events");

        try {
            // raw logged events incoming data stream from kafka
            KStream<String, JsonNode>[] rawLoggedEvents = builder.stream(stringSerde, jsonSerde, "topic_logged_events")
                    .branch(
                            (k, v) -> !v.path("anonymous_user").asBoolean(),
                            (k, v) -> v.path("anonymous_user").asBoolean()
                    );

            // parallel log for anonymous events (may want to optimise how we do this later)
            rawLoggedEvents[1].to(stringSerde, jsonSerde, "topic_anonymous_logged_events");



        // SITE STATISTICS
        DerivedStreams.userStatistics(rawLoggedEvents[0]);

        //*** BADGES & ACHIEVEMENTS ***//
        //achievementProcessor = new ThresholdAchievedProcessor(database);
        //DerivedStreams.userAchievements(rawLoggedEvents[0], contentManager, contentIndex, achievementProcessor);



        //*** BADGES & ACHIEVEMENTS ***//
        achievementProcessor = new ThresholdAchievedProcessor(database);
        DerivedStreams.userAchievements(rawLoggedEvents[0], contentManager, contentIndex, achievementProcessor);



            //use the builder and the streams configuration we set to setup and start a streams object
            streams = new KafkaStreams(builder, streamsConfiguration);
            streams.cleanUp();
            streams.start();

            // return when streams instance is initialized
            while (true) {

                if (streams.state().isRunning())
                    break;
            }
        } catch (TopologyBuilderException e) {
            log.error(e.getMessage());
        }



    }


}
