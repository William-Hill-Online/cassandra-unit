package org.cassandraunit.cli;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.transport.TTransportException;
import org.cassandraunit.CQLDataLoader;
import org.cassandraunit.dataset.cql.FileCQLDataSet;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

import java.io.File;
import java.io.IOException;

public class CassandraUnitCommandLineLoader {

    public static final String CQL_FILE_EXTENSION = "cql";
    private static CommandLineParser commandLineParser = null;

    private static Options options = null;

    private static CommandLine commandLine = null;

    private static boolean usageBeenPrinted = false;

    /**
     * @param args
     * @throws ParseException
     */
    public static void main(String[] args) {
        boolean exit = parseCommandLine(args);
        if (exit) {
            System.exit(1);
        } else {
            load();
        }

    }

    protected static boolean parseCommandLine(String[] args) {
        clearStaticAttributes();
        initOptions();
        commandLineParser = new PosixParser();
        boolean exit = false;
        try {
            commandLine = commandLineParser.parse(options, args);
            if (commandLine.getOptions().length == 0) {
                exit = true;
                printUsage();
            } else {
                if (containBadReplicationFactorArgumentValue()) {
                    printUsage("Bad argument value for option r");
                    exit = true;
                }
            }
        } catch (ParseException e) {
            printUsage(e.getMessage());
            exit = true;
        }

        return exit;

    }

    protected static void load() {
        System.out.println("Start Loading...");
        String host = commandLine.getOptionValue("h");
        String port = commandLine.getOptionValue("p");
        String file = commandLine.getOptionValue("f");
        String yamlFile = commandLine.getOptionValue("y");
        String timeout = commandLine.getOptionValue("t");

        if (yamlFile != null) {
            try {
                EmbeddedCassandraServerHelper.startEmbeddedCassandra(new File(yamlFile), "temp", Long.parseLong(timeout));
            } catch (TTransportException | IOException e) {
                e.printStackTrace();
            }
        } else {
            String fileExtension = StringUtils.substringAfterLast(file, ".");

            if (CQL_FILE_EXTENSION.equals(fileExtension)) {
                cqlDataSetLoad(host, port, file);
            } else {
                otherTypeOfDataSetLoad(host, port, file);
            }
            System.out.println("Loading completed");
        }
    }

    private static void otherTypeOfDataSetLoad(String host, String port, String file) {
        Cluster cluster = com.datastax.driver.core.Cluster.builder()
                .addContactPoints(host)
                .withPort(Integer.parseInt(port))
                .build();

        CQLDataLoader dataLoader = new CQLDataLoader(cluster.connect());
        dataLoader.load(new FileCQLDataSet(file));
    }

    private static void cqlDataSetLoad(String host, String port, String file) {
        Cluster cluster = new Cluster.Builder().addContactPoints(host).withPort(Integer.parseInt(port)).build();
        Session session = cluster.connect();
        CQLDataLoader dataLoader = new CQLDataLoader(session);
        dataLoader.load(new FileCQLDataSet(file, false));
    }

    private static boolean containBadReplicationFactorArgumentValue() {
        String replicationFactor = commandLine.getOptionValue("r");
        if (replicationFactor != null && !replicationFactor.trim().isEmpty()) {
            try {
                Integer.parseInt(replicationFactor);
                return false;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return false;
    }

    private static void printUsage(String message) {
        System.out.println(message);
        printUsage();
    }

    private static void initOptions() {
        options = new Options();
        options.addOption(OptionBuilder.withLongOpt("file").hasArg().withDescription("dataset to load").isRequired()
                .create("f"));
        options.addOption(OptionBuilder.withLongOpt("host").hasArg().withDescription("target host (required)")
                .isRequired().create("h"));
        options.addOption(OptionBuilder.withLongOpt("port").hasArg().withDescription("target port (required)")
                .isRequired().create("p"));
        options.addOption(OptionBuilder.withLongOpt("yaml").hasArg().withDescription("yaml file (required)")
                .create("y"));
        options.addOption(OptionBuilder.withLongOpt("timeout").hasArg().withDescription("start up timeout (required)")
                .create("t"));
    }

    private static void clearStaticAttributes() {
        commandLine = null;
        commandLineParser = null;
        options = null;
        usageBeenPrinted = false;
    }

    private static void printUsage() {
        usageBeenPrinted = true;
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp("CassandraUnitLoader is a tool to load CassandraUnit data Set into cassandra cluster",
                options);
    }

    protected static CommandLine getCommandLine() {
        return commandLine;
    }

    protected static boolean isUsageBeenPrinted() {
        return usageBeenPrinted;
    }
}
