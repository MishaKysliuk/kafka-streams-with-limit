package org.test.bug;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class QuickStreamRunner {

  private static final String APP_ID = "kafkatest";
  private static final String INPUT_TOPIC = "/testing:pageviews";
  private static final String KAFKA_STREAMS_DIR = "/apps/kafka-streams/";
  private static int nextStreamIndex = 0;
  private static FileSystem fileSystem;

  private static Properties props;
  static {
    props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, APP_ID);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class);
  }

  public static void initializeFs() throws IOException {
    fileSystem = FileSystem.get(new Configuration());
  }

  public static void runStream() throws InterruptedException {
    final StreamsBuilder builder = new StreamsBuilder();

    final CountDownLatch latch = new CountDownLatch(1);

    KStream<byte[], byte[]> printStream = builder.stream(INPUT_TOPIC);

    printStream.foreach((k, v) -> {
      System.out.println(String.format("Key: %s, value: %s",
          new String(k), new String(v)));
      latch.countDown();
    });

    Topology topology = builder.build();
    deletePath();
    modifyStreamIndex();

    KafkaStreams streams = new KafkaStreams(topology, props);
    System.out.println("Stream started");
    streams.start();
    latch.await();
    streams.close();
    System.out.println("Stream closed");
  }

  private static void modifyStreamIndex() {
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, String.format("%s%d", APP_ID, nextStreamIndex));
    nextStreamIndex++;
  }

  private static void deletePath() {
    String absolutePath = String.format("%s%s%d", KAFKA_STREAMS_DIR, APP_ID, nextStreamIndex);
    Path path = new Path(absolutePath);
    try {
      if (fileSystem.exists(path)) {
        System.out.println("Deleting path " + absolutePath);
        fileSystem.delete(path, true);
      }
    } catch (IOException e) {
      System.out.println("Could not delete path " + absolutePath);
    }
  }


}
