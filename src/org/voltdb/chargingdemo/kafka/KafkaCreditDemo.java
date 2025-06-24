/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.chargingdemo.kafka;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaCreditDemo {

    private static final String QUOTE_COMMA_QUOTE = "\",\"";
    private static final String SINGLE_QUOTE = "\"";

    public static void main(String[] args) throws UnknownHostException {

        msg("Parameters:" + Arrays.toString(args));

        if (args.length != 5) {
            msg("Usage: kafkaserverplusport recordcount  tpms durationseconds maxamount");
            System.exit(1);
        }

        String kafkaserverplusport = args[0];
        int recordCount = 0;
        int tpms = 0;
        int durationseconds = 0;
        int maxamount = 0;
        Random r = new Random();

        try {
            recordCount = Integer.parseInt(args[1]);
            tpms = Integer.parseInt(args[2]);
            durationseconds = Integer.parseInt(args[3]);
            maxamount = Integer.parseInt(args[4]);

        } catch (NumberFormatException e) {
            msg("Value should be a number:" + e.getMessage());
            System.exit(1);
        }

        Properties config = new Properties();
        config.put("client.id", InetAddress.getLocalHost().getHostName());
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaserverplusport);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        config.put("acks", "all");

        KafkaProducer<String, String> producer = new KafkaProducer<>(config);

        long endtimeMs = System.currentTimeMillis() + (1000 * durationseconds);
        // How many transactions we've done...

        int tranCount = 0;

        int tpThisMs = 0;
        long currentMs = System.currentTimeMillis();

        while (endtimeMs > System.currentTimeMillis()) {

            if (tpThisMs++ > tpms) {

                while (currentMs == System.currentTimeMillis()) {
                    try {
                        Thread.sleep(0, 50000);
                    } catch (InterruptedException e) {
                    }

                }

                currentMs = System.currentTimeMillis();
                tpThisMs = 0;
            }

            int userId = r.nextInt(recordCount);
            int amount = r.nextInt(maxamount);
            String txnId = "Kafka_" + tranCount + "_" + currentMs;
            String request = SINGLE_QUOTE + userId + QUOTE_COMMA_QUOTE + amount + QUOTE_COMMA_QUOTE + txnId
                    + SINGLE_QUOTE;

            ProducerRecord<String, String> newrec = new ProducerRecord<>("ADDCREDIT", txnId, request);

            Future<RecordMetadata> theFuture = producer.send(newrec);

            if (tranCount++ % 10000 == 0) {
                while (!theFuture.isDone()) {
                    try {
                        Thread.sleep(0, 50000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                try {
                    RecordMetadata rmd = theFuture.get();
                    msg(rmd.toString());
                } catch (InterruptedException | ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                msg("On transaction# " + tranCount + ", user,amount,txnid= " + request);
            }

        }

        producer.flush();
        producer.close();

    }

    /**
     * Print a formatted message.
     *
     * @param message
     */
    public static void msg(String message) {

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + message);

    }

}
