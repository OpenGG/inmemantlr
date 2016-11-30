/**
 * Inmemantlr - In memory compiler for Antlr 4
 * <p>
 * The MIT License (MIT)
 * <p>
 * Copyright (c) 2016 Julian Thome <julian.thome.de@gmail.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 **/


package org.snt.inmemantlr;

import org.apache.commons.cli.*;
import org.slf4j.LoggerFactory;
import org.snt.inmemantlr.listener.DefaultTreeListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Inmemantlr {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Inmemantlr.class);


    // just in case we would add wildcard/regex support later on
    private static List<File> getListOfFiles(String file) {
        List<File> ret = new Vector();
        ret.add(new File(file));
        return ret;
    }

    public static void main(String[] args) {

        LOGGER.info("Inmemantlr tool");

        HelpFormatter hformatter = new HelpFormatter();

        Options options = new Options();

        // Binary arguments
        options.addOption("h", "print this message");

        Option ufils = Option.builder("util")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .longOpt("utilility-files")
                .desc("comma-separated list of utility files required for " +
                        "compilation")
                .valueSeparator(',')
                .argName("util")
                .required(false)
                .build();

        Option infiles = Option.builder("in")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .longOpt("input-files")
                .desc("comma-separated list of files to parse")
                .valueSeparator(',')
                .required(true)
                .build();

        Option gfiles = Option.builder("grmr")
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .longOpt("grammar-files")
                .desc("comma-separated list of ANTLRv4 gramar files (.g4)")
                .valueSeparator(',')
                .argName("grmr")
                .required(true)
                .build();

        Option odir = Option.builder("odir")
                .numberOfArgs(1)
                .longOpt("output-dir")
                .desc("output directory to which parse trees are saved")
                .required(false)
                .argName("odir")
                .build();

        Option force = Option.builder("f")
                .numberOfArgs(0)
                .longOpt("-force-overwrite")
                .desc("force write")
                .valueSeparator(',')
                .required(false)
                .build();


        options.addOption(ufils);
        options.addOption(gfiles);
        options.addOption(infiles);
        options.addOption(odir);
        options.addOption(force);

        CommandLineParser parser = new DefaultParser();

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption('h')) {
                hformatter.printHelp("java -jar inmemantlr.jar", options);
                System.exit(0);
            }
        } catch (ParseException e) {
            hformatter.printHelp("java -jar inmemantlr.jar", options);
            LOGGER.error(e.getMessage());
            System.exit(-1);
        }

        // input files
        Set<String> ins = new HashSet();
        ins.addAll(Arrays.asList(cmd.getOptionValues("in")));
        Set<File> infs = new HashSet();
        ins.stream().map(v -> getListOfFiles(v)).forEach(infs::addAll);

        assert infs.size() > 0;

        // grammar files
        Set<String> gs = new HashSet();
        gs.addAll(Arrays.asList(cmd.getOptionValues("grmr")));
        Set<File> gfs = new HashSet();
        gs.stream().map(v -> getListOfFiles(v)).forEach(gfs::addAll);


        assert gfs.size() > 0;

        // utility files
        Set<File> uf = new HashSet();

        if(cmd.hasOption("util")) {
            Set<String> us = new HashSet();
            us.addAll(Arrays.asList(cmd.getOptionValues("util")));
            us.stream().map(v -> getListOfFiles(v)).forEach(uf::addAll);
        }

        // output dir
        String os = cmd.getOptionValue("odir");
        File ofs = new File(os);

        LOGGER.info("create generic parser");

        GenericParser gp = null;
        try {
            gp = new GenericParser(gfs.toArray(new File[gfs.size()]));
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            System.exit(-1);
        }

        for(File f : uf){
            try {
                gp.addUtilityJavaFile(f);
            } catch (FileNotFoundException e) {
                LOGGER.error(e.getMessage());
                System.exit(-1);
            }
        }

        LOGGER.info("create and add parse tree listener");
        DefaultTreeListener dt = new DefaultTreeListener();
        gp.setListener(dt);

        LOGGER.info("compile generic parser");
        if(!gp.compile()) {
            LOGGER.error("could not compile generic parser");
            System.exit(-1);
        }

    }

}
