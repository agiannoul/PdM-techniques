package mtree.tests;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

import be.tarsos.lsh.Vector;
import com.sun.xml.internal.ws.api.ha.StickyFeature;
import outlierdetection.MicroCluster;
import mtree.utils.Constants;
import mtree.utils.Utils;

import outlierdetection.MicroCluster_New;

import static mtree.utils.Constants.bus;

public class MTTestBus {

    public static int currentTime = 0;

    private static int globalcounter=0;

    public static boolean stop = false;

    public static HashSet<String> idOutliers = new HashSet<>();
    public static HashMap<Integer,String> idOutliersname = new HashMap<>();


    public static String algorithm;
    public static Date currentRealTime;

    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException {
        String[] Busses={"369","370","371","372","373","374","375","376","377","378","379","380","381","382","383","452","453","454","455"};

        //readArguments(args);

            currentTime = 0;

            globalcounter=0;

            stop = false;

            idOutliers = new HashSet<>();
            idOutliersname = new HashMap<>();


            setArguments();
            MesureMemoryThread mesureThread = new MesureMemoryThread();
            mesureThread.start();
//         Stream s = Stream.getInstance("ForestCover");
            Stream s = Stream.getInstance("");
//         Stream s = Stream.getInstance("randomData");
//        Stream s = Stream.getInstance("randomData1");
            // Stream s = Stream.getInstance(null);
            // Stream s = Stream.getInstance("tagData");
//        Stream s = Stream.getInstance("Trade");

            MicroCluster micro = new MicroCluster();
            MicroCluster_New mcnew = new MicroCluster_New();
            int numberWindows = 0;
            double totalTime = 0;

            s.loadData(Constants.dataFile, false, false);
            //s.showcount();
            while (!stop) {

                if (Constants.numberWindow != -1 && numberWindows > Constants.numberWindow) {
                    break;
                }
                numberWindows++;

                ArrayList<Data> incomingData;

                if (currentTime != 0) {
                    incomingData = s.MygetIncomingData(currentTime, Constants.slide, Constants.dataFile, Constants.bus);
                    currentTime = currentTime + Constants.slide;
                } else {
                    incomingData = s.MygetIncomingData(currentTime, Constants.W, Constants.dataFile, Constants.bus);
                    currentTime = currentTime + Constants.W;
                }
//            currentRealTime = s.getFirstTimeStamp(Constants.dataFile);
//
//
//            if (!currentRealTime.equals(s.getFirstTimeStamp(Constants.dataFile))) {
//                incomingData = s.getTimeBasedIncomingData(currentRealTime, Constants.slide, Constants.dataFile);
//                currentRealTime.setTime(currentRealTime.getTime()+ Constants.slide*1000);
//
//
//            } else {
//                incomingData = s.getTimeBasedIncomingData(currentRealTime, Constants.slide, Constants.dataFile);
//                currentRealTime.setTime(currentRealTime.getTime()+ Constants.W*1000);
//
//            }

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
                            if (!idOutliers.contains(outlier.arrivalTime) && outlier.nameTo.contains("Buss " + Constants.showbus)) {
                                //System.out.println(outlier.nameTo.split(" ")[2]);

                            }
                            if (!idOutliers.contains(outlier.arrivalTime + "@" + outlier.nameTo)) {
                                idOutliers.add(outlier.arrivalTime + "@" + outlier.nameTo);
                                globalcounter++;
                                idOutliersname.put(globalcounter, outlier.nameTo);
                            }

                        });

                        break;
                    case "microCluster_new":
                        ArrayList<Data> outliers9 = mcnew.detectOutlier(incomingData, currentTime, Constants.W,
                                Constants.slide);
                        elapsedTimeInSec = (Utils.getCPUTime() - start) * 1.0 / 1000000000;

                        totalTime += elapsedTimeInSec;
                        outliers9.stream().forEach((outlier) -> {
                            if (!idOutliers.contains(outlier.arrivalTime) && outlier.nameTo.contains("Buss " + Constants.showbus)) {
                               // System.out.println(outlier.nameTo.split(" ")[2]);
                            }
                            idOutliers.add(outlier.arrivalTime + "@" + outlier.nameTo);

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
            /*
            System.out.println("#window: " + numberWindows);
            System.out.println("Total #outliers: " + idOutliers.size());
            System.out.println("Average Time: " + totalTime * 1.0 / numberWindows);
            System.out.println("Peak memory: " + MesureMemoryThread.maxMemory * 1.0 / 1024 / 1024);
            System.out.println("Time index, remove data from structure: " + MesureMemoryThread.timeForIndexing * 1.0 / 1000000000 / numberWindows);
            System.out.println("Time for querying: " + MesureMemoryThread.timeForQuerying * 1.0 / 1000000000 / numberWindows);
            System.out.println("Time for new slide: " + MesureMemoryThread.timeForNewSlide * 1.0 / 1000000000 / numberWindows);
            System.out.println("Time for expired slide: " + MesureMemoryThread.timeForExpireSlide * 1.0 / 1000000000 / numberWindows);
            System.out.println("------------------------------------");

            if (algorithm.equals("microCluster")) {

                System.out.println("Number clusters = " + MicroCluster.numberCluster / numberWindows);
                System.out.println("Max  Number points in event queue = " + MicroCluster.numberPointsInEventQueue);

                System.out.println("Avg number points in clusters= " + MicroCluster.numberPointsInClustersAllWindows / numberWindows);
                System.out.println("Avg Rmc size = " + MicroCluster.avgPointsInRmcAllWindows / numberWindows);
                System.out.println("Avg Length exps= " + MicroCluster.avgLengthExpsAllWindows / numberWindows);
            }
            if (algorithm.equals("microCluster_new")) {
                System.out.println("avg points in clusters = " + MicroCluster_New.avgNumPointsInClusters * 1.0 / numberWindows);
                System.out.println("Avg points in event queue = " + MicroCluster_New.avgNumPointsInEventQueue * 1.0 / numberWindows);
                System.out.println("avg neighbor list length = " + MicroCluster_New.avgNeighborListLength * 1.0 / numberWindows);
            }
            */

            }


//
//        Constants.numberWindow--;



            /**
             * Write result to file
             */
            // create folder with one txt file for every bus witch include the outliers
            //writeTofiles();


            //SHOW RESULTS
        for(String bus : Busses) {
            //if(!bus.equals(Constants.bus))continue;
            System.out.println("BUS "+bus);
            for(Integer i : idOutliersname.keySet()){
                String temp =idOutliersname.get(i);
                if(temp.contains("Buss "+ bus)){
                    System.out.println("\t"+temp.split(" ")[2]);
                }
            }
        }


        if (!"".equals(Constants.resultFile)) {
            writeResult();
        }
//      
    }

    private static void writeTofiles(){
        String[] Busses={"369","370","371","372","373","374","375","376","377","378","379","380","381","382","383","452","453","454","455"};
        try { 
            String only="";
            if(!bus.equals("")){
                only="only";
            }
            if(Constants.metric.equals("HELINGER")){
                only="HELINGER";
            }
            Path path = Paths.get("C:\\Users\\panos\\Desktop\\ptyxiakh\\apotelesmataMicrocluster\\dates_"+only+"_W"+Constants.W+"_slide"+Constants.slide+"_k"+Constants.k+"_R"+Constants.R);

            //java.nio.file.Files;

                Files.createDirectories(path);
                System.out.println("Directory is created!");
                System.out.println(path);





            for(String bus : Busses) {
                //if(!bus.equals(Constants.bus))continue;
                System.out.println("BUS "+bus);
                File file = new File(path.toString() + "\\dates"+bus+".txt");
                FileWriter myWriter = new FileWriter(file);

                for(Integer i : idOutliersname.keySet()){
                    String temp =idOutliersname.get(i);
                    if(temp.contains("Buss "+ bus)){
                        System.out.println("\t"+temp.split(" ")[2]+"\n");
                        myWriter.write(temp.split(" ")[2]+"\n");
                    }

                }


                myWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setArguments(){
        algorithm="microCluster";
        Constants.W = Integer.valueOf(30);
        Constants.slide = Integer.valueOf(15);
        Constants.k = Integer.valueOf(5);
        Constants.R = Double.valueOf(1);
        Constants.numberWindow = 120;
        Constants.showbus="";
        Constants.metric="";
        // Path of bus data folder
        Constants.dataFile = Constants.bussefile;
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
            for (String time : idOutliers) {
                out.println(time.split("@")[0]);
            }
        } catch (IOException e) {
        }

    }
}
