package mtree.tests;

import be.tarsos.lsh.Vector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;


import mtree.tests.Data;
import mtree.tests.MesureMemoryThread;
import mtree.tests.Stream;
import outlierdetection.MicroCluster;
import mtree.utils.Constants;
import mtree.utils.Utils;

import outlierdetection.MicroCluster_New;
import sun.awt.windows.WPrinterJob;

public class MTTest {

    public static int currentTime = 0;

    public static boolean stop = false;

    public static HashSet<Integer> idOutliers = new HashSet<>();

    public static String algorithm;
    public static Date currentRealTime;
    private  static HashSet<String> outliers=new HashSet<>();
    public static int TruePos=0;
    public static int FalsePos=0;
    private  static HashMap<String,Double> RESULTS= new HashMap<>();
    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {

                    idOutliers.clear();
                    outliers.clear();
                    idOutliers = new HashSet<>();
                    outliers = new HashSet<>();
                    //readArguments(args);
                    setArguments();
                    System.out.println("R="+Constants.R+" k="+Constants.k+" W="+Constants.W/(60));

                    MesureMemoryThread mesureThread = new MesureMemoryThread();
                    mesureThread.start();
//         Stream s = Stream.getInstance("ForestCover");
                    Stream s = Stream.getInstance("vowels");
//         Stream s = Stream.getInstance("randomData");
//        Stream s = Stream.getInstance("randomData1");
                    // Stream s = Stream.getInstance(null);
                    // Stream s = Stream.getInstance("tagData");
//        Stream s = Stream.getInstance("Trade");


                    MicroCluster micro = new MicroCluster();
                    MicroCluster_New mcnew = new MicroCluster_New();
                    int numberWindows = 0;
                    double totalTime = 0;

                    currentRealTime = s.getFirstTimeStamp(Constants.dataFile);
                    while (!stop) {

                        if (Constants.numberWindow != -1 && numberWindows > Constants.numberWindow) {
                            break;
                        }
                        numberWindows++;

                        ArrayList<Data> incomingData;
//            if (currentTime != 0) {
//                incomingData = s.getIncomingData(currentTime, Constants.slide, Constants.dataFile);
//                currentTime = currentTime + Constants.slide;
//            } else {
//                incomingData = s.getIncomingData(currentTime, Constants.W, Constants.dataFile);
//                currentTime = currentTime + Constants.W;
//            }


                        if (!currentRealTime.equals(s.getFirstTimeStamp(Constants.dataFile))) {
                            incomingData = s.getTimeBasedIncomingData(currentRealTime, Constants.slide, Constants.dataFile);
                            currentRealTime.setTime(currentRealTime.getTime() + Constants.slide * 1000);
                        } else {
                            incomingData = s.getTimeBasedIncomingData(currentRealTime, Constants.slide, Constants.dataFile);
                            currentRealTime.setTime(currentRealTime.getTime() + Constants.W * 1000);

                        }

                        long start = Utils.getCPUTime(); // requires java 1.5

                        /**
                         * do algorithm
                         */
                        switch (algorithm) {

                            case "microCluster":
                                ArrayList<Data> outliers6 = micro.detectOutlier(incomingData, currentTime, Constants.W,
                                        Constants.slide);
                                double elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                                totalTime += elapsedTimeInSec;
                                outliers6.stream().forEach((outlier) -> {
                                    idOutliers.add(outlier.arrivalTime);
                                    String out1 = outlier.nameTo;
                                    if (!outliers.contains(out1)) {
                                        System.out.println(out1);
                                        outliers.add(out1);
                                    }
                                });

                                break;
                            case "microCluster_new":
                                ArrayList<Data> outliers9 = mcnew.detectOutlier(incomingData, currentTime, Constants.W,
                                        Constants.slide);
                                elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                                totalTime += elapsedTimeInSec;
                                outliers9.stream().forEach((outlier) -> {
                                    idOutliers.add(outlier.arrivalTime);

                                });

//                    ArrayList<Data> outliers10 = estorm.detectOutlier(incomingData, currentTime, Constants.W,
//                            Constants.slide);
//
//                    System.out.println("--------------------------------------------------");
//                    System.out.println("Not in exact storm");
//                    for(Data d: outliers9){
//                        if(!outliers10.contains(d))
//                            System.out.println(d.arrivalTime);
//                    }
//                    System.out.println("---------------------------------------------------");
                                break;


                        }
                        if (numberWindows == 1) {
                            totalTime = 0;
                            MesureMemoryThread.timeForIndexing = 0;
                            MesureMemoryThread.timeForNewSlide = 0;
                            MesureMemoryThread.timeForExpireSlide = 0;
                        }
                    }

        //tofoTaxi();
        //tpfpambientTemp();
        //tpfpMachineTemp();
        tpfpCpuLatncy();


        if (!"".equals(Constants.resultFile)) {
            writeResult();

        }


    }

    private static void tpfpambientTemp(){
        String[] outliersdates = {"2013-12-22 20:00:00","2014-04-13 09:00:00"};
        int tp=0;
        HashSet<String> foundou=new HashSet<>();
        int fp=0;
        //String dateingore="2013-12-05 23:55:00";

        try {
            for (String reporte : outliers) {
                //Date ignore=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateingore);;
                Date datereporte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(reporte);

                //if(datereporte.before(ignore))continue;

                boolean istp = false;
                for (String out : outliersdates) {
                    Date dateout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(out);
                    Date datelimit = new Date(dateout.getTime()-3600*1000*Constants.phours);
                    if ((datereporte.before(dateout) && datereporte.after(datelimit)) || datereporte.equals(dateout) ){
                        foundou.add(out);
                        istp = true;
                    }
                }
                if (istp) {
                    tp++;
                } else {
                    fp++;
                    //System.out.println("FALSE: "+reporte);
                }
            }
        }catch (Exception o){

        }
        double acc = 1.0 * tp / (tp + fp);
        double recall = 1.0 *foundou.size() / (outliersdates.length);
        double f1=0;
        if(recall==0 || acc==0){
            f1=0;
        }else{
            f1 = 2.0 / ((1 / acc) + (1 / recall));
        }
        System.out.println(" Precison=" + acc);
        System.out.println(" Recall=" + recall);
        System.out.println(" F1=" + f1);
        String messge = "\nMCOD R=" + Constants.R + " k=" + Constants.k + " W=" + (Constants.W/3600)+" Slide=" + (Constants.slide/3600)+ " Acc=" + acc + " Recall=" + recall + " F1=" + f1;
        mywriteresults("\\ambient_temperature\\",messge);
    }

    private static void tpfpMachineTemp(){
        String[] outliersdates = {"2013-12-11 06:00:00","2013-12-16 17:25:00","2014-01-28 13:55:00","2014-02-08 14:30:00"};
        int tp=0;
        HashSet<String> foundou=new HashSet<>();
        int fp=0;
        String dateingore="2013-12-05 23:55:00";

        try {
            for (String reporte : outliers) {
                Date ignore=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateingore);;
                Date datereporte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(reporte);

                //if(datereporte.before(ignore))continue;

                boolean istp = false;
                for (String out : outliersdates) {
                    Date dateout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(out);
                    Date datelimit = new Date(dateout.getTime()-3600*1000*Constants.phours);
                    if ((datereporte.before(dateout) && datereporte.after(datelimit)) || datereporte.equals(dateout) ){
                        foundou.add(out);
                        istp = true;
                    }
                }
                if (istp) {
                    tp++;
                } else {
                    fp++;
                    //System.out.println("FALSE: "+reporte);
                }
            }
        }catch (Exception o){

        }
        double acc = 1.0 * tp / (tp + fp);
        double recall = 1.0 *foundou.size() / (outliersdates.length);
        double f1=0;
        if(recall==0 || acc==0){
            f1=0;
        }else{
            f1 = 2.0 / ((1 / acc) + (1 / recall));
        }
        System.out.println(" Precison=" + acc);
        System.out.println(" Recall=" + recall);
        System.out.println(" F1=" + f1);
        String messge = "\nMCOD R=" + Constants.R + " k=" + Constants.k + " W=" + (Constants.W/60)+ " Acc=" + acc + " Recall=" + recall + " F1=" + f1;
        mywriteresults("\\machineTemperatur\\",messge);
    }
    //for CPU LATANCY TEST
    private static void tpfpCpuLatncy(){
        String[] outliersdates = {"2014-03-14 09:06:00",
                                  "2014-03-18 22:41:00",
                                  "2014-03-21 03:01:00"};
        int tp=0;
        int fp=0;
        for (String reporte : outliers) {
            boolean istp=false;
            for (String out : outliersdates) {
                if(reporte.equals(out)){
                    istp=true;
                }
            }
            if(istp){
                tp++;
            }else {
                fp++;
            }
        }

        double acc = 1.0 * tp / (tp + fp);
        double recall = 1.0 * tp / (outliersdates.length);
        double f1=0;
        if(recall==0 || acc==0){
            f1=0;
        }else{
            f1 = 2.0 / ((1 / acc) + (1 / recall));
        }
        System.out.println(" Precison=" + acc);
        System.out.println(" Recall=" + recall);
        System.out.println(" F1=" + f1);
        String messge = "\nMCOD R=" + Constants.R + " k=" + Constants.k + " W=" + (Constants.W/60)+ " Acc=" + acc + " Recall=" + recall + " F1=" + f1;
        mywriteresults("ec2_recuest_ltancy\\",messge);
    }
    //For taxi test
    private  static  void tofoTaxi(){
        String[] outliersdates = {"2014-11-02", "2014-11-27", "2014-12-25", "2015-01-01"};
        String[] Judodates = {"2015-01-23", "2015-01-24", "2015-01-25", "2015-01-26", "2015-01-27"};
        int tp = 0;
        int fp = 0;
        double acc=0;
        double recall=0;
        boolean judostorm = false;
        for (String reporte : outliers) {
            boolean istp = false;
            for (String out : outliersdates) {
                if (out.equals(reporte.split(" ")[0])) {
                    istp = true;
                }

            }
            boolean judodate = false;
            for (String out : Judodates) {
                if (out.equals(reporte)) {
                    judostorm = true;
                    judodate = true;
                }
            }

            if (istp) {
                tp++;
            } else if (!judodate) {
                fp++;
            }
        }
        if (judostorm) {
            tp++;
        }
        acc = 1.0 * tp / (tp + fp);
        recall = 1.0 * tp / (outliersdates.length + 1);
        double f1=0;
        if(recall==0 || acc==0){
            f1=0;
        }else{
            f1 = 2.0 / ((1 / acc) + (1 / recall));
        }
        System.out.println(" Precison=" + acc);
        System.out.println(" Recall=" + recall);
        System.out.println(" F1=" + f1);
        String messge = "\nMCOD R=" + Constants.R + " k=" + Constants.k + " W=" + (Constants.W / (3600 * 24))+ " Acc=" + acc + " Recall=" + recall + " F1=" + f1;
        //System.out.println("Accuracy: "+acc+"\nRecall: "+recall);
        /**
         * Write result to file
         */
    }

    private static void mywriteresults(String pathfolder,String message) {
        Path path = Paths.get(Constants.matpath+pathfolder+"test.txt");
        try {
            Files.write(path, message.getBytes(), StandardOpenOption.APPEND);  //Append mode
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setArguments(){
        algorithm="microCluster";
        //path to file with data
        String filename=Constants.matpath+"ec2_recuest_ltancy\\cpulatancy.csv";

            //Constants.outliers=countOut(filename);
            //System.out.println("Outliers: "+Constants.outliers);
            //int rows=count(filename);

            //W in seconds
            Constants.W = Integer.valueOf(125*60);
            Constants.slide =Constants.W/2;
            Constants.k = Integer.valueOf(1);
            Constants.R = Double.valueOf(4);
            Constants.numberWindow = 500;
            Constants.showbus="";
            Constants.metric="";
            Constants.dataFile = filename;
            Constants.phours=8;


    }

    private static int countOut(String filename) throws IOException {
        String line = "";
        String splitBy = ",";
        int count=0;
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try
        {
        //parsing a CSV file into BufferedReader class constructor
            boolean first=true;
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)   //returns a Boolean value
            {
                if(first){
                    first=false;
                    continue;
                }
                String[] features = line.split(splitBy);    // use comma as separator
                if( features[features.length-1].equals("1.0")){
                    Constants.outlierIndex.add(true);
                    count++;
                }else {
                    Constants.outlierIndex.add(false);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return count;
    }

    private static int count(String filename) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(filename));
        try {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while ((readChars = is.read(c)) != -1) {
                empty = false;
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return (count == 0 && !empty) ? 1 : count;
        } finally {
            is.close();
        }
    }

    public static void readArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {

            //check if arg starts with --
            String arg = args[i];
            if (arg.indexOf("--") == 0) {
                switch (arg) {
                    case "--algorithm":
                        algorithm = args[i + 1];
                        break;
                    case "--R":
                        Constants.R = Double.valueOf(args[i + 1]);
                        break;
                    case "--W":
                        Constants.W = Integer.valueOf(args[i + 1]);
                        break;
                    case "--k":
                        Constants.k = Integer.valueOf(args[i + 1]);
                        break;
                    case "--datafile":
                        Constants.dataFile = args[i + 1];
                        break;
                    case "--output":
                        Constants.outputFile = args[i + 1];
                        break;
                    case "--numberWindow":
                        Constants.numberWindow = Integer.valueOf(args[i + 1]);
                        break;
                    case "--slide":
                        Constants.slide = Integer.valueOf(args[i + 1]);
                        break;
                    case "--resultFile":
                        Constants.resultFile = args[i + 1];
                        break;
                    case "--samplingTime":
                        Constants.samplingPeriod = Integer.valueOf(args[i + 1]);
                        break;
                    case "--likely":
                        Constants.likely = Double.valueOf(args[i + 1]);
                        break;

                }
            }
        }
    }

    public static void writeResult() {

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Constants.resultFile, true)))) {
            for (Integer time : idOutliers) {
                out.println(time);
            }
        } catch (IOException e) {
        }

    }
}
