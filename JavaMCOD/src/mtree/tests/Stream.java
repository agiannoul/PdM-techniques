package mtree.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import mtree.utils.Constants;

import javax.swing.*;

import static java.lang.Math.sqrt;

public class Stream {

    PriorityQueue<Data> streams;

    public static Stream streamInstance;

    private ArrayList<Data> All = new ArrayList<>();

    public static Stream getInstance(String type) {

        if (streamInstance != null) {
            return streamInstance;
        } else if (!Constants.dataFile.trim().equals("")) {
            streamInstance = new Stream();
//            streamInstance.getData(Constants.dataFile);
            return streamInstance;
        } else if ("ForestCover".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.forestCoverFileName);
            return streamInstance;
        }else if("vowels".equals(type)){
            streamInstance = new Stream();
            streamInstance.getData(Constants.vowels);
            return streamInstance;
        } else if ("TAO".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.taoFileName);
            return streamInstance;
        } else if ("randomData".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.randomFileName1111);
            return streamInstance;
        } else if ("randomData0.001".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.randomFileName001);
            return streamInstance;
        } else if ("randomData0.01".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.randomFileName01);
            return streamInstance;
        } else if ("randomData0.1".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.randomFileName1);
            return streamInstance;
        } else if ("randomData1".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.randomFileName1percent);
            return streamInstance;
        } else if ("randomData10".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.randomFileName10percent);
            return streamInstance;
        } else if ("tagData".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.tagCALC);
            return streamInstance;
        } else if ("Trade".equals(type)) {
            streamInstance = new Stream();
            streamInstance.getData(Constants.STT);
            return streamInstance;
        } else {
            streamInstance = new Stream();
            streamInstance.getRandomInput(1000, 10);
            return streamInstance;

        }

    }

    public boolean hasNext() {
        return !streams.isEmpty();
    }

    public void loadData(String filenames,boolean normalize_mean,boolean normalize_std){
        File folder = new File(filenames);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> files = new ArrayList<>();
        for(File a : listOfFiles){
            files.add(a.getAbsolutePath());
        }
        int counter=0;
        for(String filename: files) {
            counter++;
            try {
                BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

                String line = "";
                int time = 0;
                try {
                    bfr.readLine();
                    ArrayList<Data> current= new ArrayList<>();
                    while ((line = bfr.readLine()) != null) {
                        time++;
                        String[] atts = line.split(",");
                        double[] d = new double[atts.length-1];
                        if(atts.length<51)continue;
                        if(atts[2]=="")continue;
                        for (int i = 1; i < d.length+1; i++) {

                            d[i-1] = Double.valueOf(atts[i]) + (new Random()).nextDouble() / 10000000;

                        }
                        //System.out.println(d[30]);
                        Data data = new Data(d);
                        char[] bussname=filename.split("_")[0].toCharArray();
                        data.nameTo= "Buss " + bussname[bussname.length-3]+bussname[bussname.length-2]+bussname[bussname.length-1]   + " " + atts[0];
                        data.arrivalTime = time;

                        current.add(data);

                    }
                    double[] means =new  double[current.get(0).values.length] ;

                    if(normalize_mean){
                        for(int i = 0 ; i<means.length;i++){
                            means[i]=0;
                        }

                        for(Data d : current){
                            int i=0;
                            for(double value : d.values){
                                means[i]+=value/current.size();;
                                i++;
                            }
                        }

                        for(int i=0;i<current.size();i++){
                            double[] temp = new double[means.length];
                            for(int j = 0 ; j<means.length;j++){
                                temp[j]=current.get(i).values[j]-means[j];
                            }
                            current.set(i,current.get(i).setValues(temp));
                        }
                    }

                    if(normalize_std){
                        double[] std =new  double[current.get(0).values.length] ;
                        for(int i = 0 ; i<std.length;i++){
                            std[i]=0;
                        }
                        for(Data d : current){
                            int i=0;
                            for(double value : d.values){
                                std[i]+=(value-means[i])*(value-means[i])/current.size();
                                i++;
                            }
                        }
                        for(int i=0;i<std.length;i++){
                            std[i]=sqrt(std[i]);
                            if(std[i]==0)std[i]=1;
                        }

                        for(int i=0;i<current.size();i++){
                            double[] temp = new double[means.length];
                            for(int j = 0 ; j<std.length;j++){
                                temp[j]=current.get(i).values[j]/std[j];
                            }
                            current.set(i,current.get(i).setValues(temp));
                        }

                    }

                    All.addAll(current);
                    bfr.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public ArrayList<Data> MygetIncomingData(int currentTime, int length, String filenames,String bus) {

        ArrayList<Data> results = new ArrayList<>();


         for(Data data : All) {
            // if(data.nameTo.contains("Buss "+bus))continue;
            if(data.arrivalTime> currentTime && data.arrivalTime <= currentTime + length) {
                results.add(data);
            }
         }
        return results;
    }

    public void showcount(){
        int[] Count={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        for(Data data : All) {
            int pos =Integer.parseInt(data.nameTo.split(" ")[1]);
            if(pos > 400){
                Count[pos-452+15]++;
            }
            else{
                Count[pos-369]++;
            }

        }
        for(int c : Count){
            System.out.println(c);
        }
    }
    public ArrayList<Data> getIncomingData(int currentTime, int length, String filename) {

        ArrayList<Data> results = new ArrayList<>();
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 0;
            try {
                //for titles
                bfr.readLine();
                while ((line = bfr.readLine()) != null) {
                    time++;
                    if (time > currentTime && time <= currentTime + length) {
                        String[] atts = line.split(",");
                        double[] d = new double[atts.length-1];
                        for (int i = 0; i < d.length; i++) {

                            d[i] = Double.valueOf(atts[i+1]);
                        }
                        Data data = new Data(d);
                        data.arrivalTime = time;
                        data.nameTo=atts[0];
                        results.add(data);
                    }
                }
                bfr.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;
    }

    public Date getFirstTimeStamp(String filename) throws FileNotFoundException, IOException, ParseException {
        BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

        String line = "";
        line = bfr.readLine();
        line=bfr.readLine();
        String[] atts = line.split(",");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date data_time = formatter.parse(atts[0].trim());
        return data_time;
    }
    public Date getFirstTimeStampMultipleFiles(String filenames) throws FileNotFoundException, IOException, ParseException {

        File folder = new File(filenames);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> files = new ArrayList<>();
        for(File a : listOfFiles){
            files.add(a.getAbsolutePath());
        }

        BufferedReader bfr = new BufferedReader(new FileReader(new File(files.get(0))));

        String line = "";
        line = bfr.readLine();
        line = bfr.readLine();
        String[] atts = line.split(",");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date data_time = formatter.parse(atts[0].trim());
        return data_time;
    }



    public ArrayList<Data> getRandomIncomingData(int currentTime, int length, String filename, double likely) {
        Random r = new Random();
        ArrayList<Data> results = new ArrayList<>();
        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 0;
            try {
                while ((line = bfr.readLine()) != null) {
                    time++;
                    if (time > currentTime && time <= currentTime + length) {
                        String[] atts = line.split(",");
                        double[] d = new double[atts.length];
                        for (int i = 0; i < d.length; i++) {

                            d[i] = Double.valueOf(atts[i]) + (new Random()).nextDouble() / 10000000;
                        }
                        Data data = new Data(d);
                        data.arrivalTime = time;

                        if (likely > r.nextDouble()) {
                            results.add(data);
                        }
                    }
                }
                bfr.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return results;

    }

    public ArrayList<Data> getTimeBasedIncomingData(Date currentTime, int lengthInSecond, String filename) {
        ArrayList<Data> results = new ArrayList<>();



        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 0;
            Date endTime = new Date();
            endTime.setTime(currentTime.getTime() + lengthInSecond * 1000);
            try {
                line=bfr.readLine();
                while ((line = bfr.readLine()) != null) {
                    String[] atts = line.split(",");
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    try {
                        Date data_time = formatter.parse(atts[0].trim());
                        if (data_time.after(currentTime) && data_time.before(endTime)) {

                            double[] d = new double[atts.length];
                            for (int i = 1; i < d.length; i++) {

                                d[i - 1] = Double.valueOf(atts[i]) ;
                            }
                            Data data = new Data(d);

                            data.arrivalTime = time;
                            data.nameTo=String.valueOf(atts[0]);
                            results.add(data);

                        }
                    } catch (ParseException ex) {
                        Logger.getLogger(Stream.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                bfr.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }





        return results;

    }



    public ArrayList<Data> getTimeBasedIncomingDataMultiplefiles(Date currentTime, int lengthInSecond, String filenames) {
        ArrayList<Data> results = new ArrayList<>();


        File folder = new File(filenames);
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> files = new ArrayList<>();
        for(File a : listOfFiles){
            files.add(a.getAbsolutePath());
        }
        for(String filename: files) {

            try {
                BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

                String line = "";
                int time = 0;
                Date endTime = new Date();
                endTime.setTime(currentTime.getTime() + lengthInSecond * 1000);
                try {
                    bfr.readLine();
                    while ((line = bfr.readLine()) != null) {
                        String[] atts = line.split(",");
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                        try {
                            Date data_time = formatter.parse(atts[0].trim());
                            if (data_time.after(currentTime) && data_time.before(endTime)) {

                                double[] d = new double[atts.length - 1];
                                for (int i = 1; i < d.length; i++) {

                                    d[i - 1] = Double.valueOf(atts[i]) ;//+ (new Random()).nextDouble() / 10000000;
                                }
                                Data data = new Data(d);
                                data.arrivalTime = time;

                                results.add(data);

                            }
                        } catch (ParseException ex) {
                            Logger.getLogger(Stream.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    bfr.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }



        return results;

    }

    public ArrayList<Data> getIncomingData(int currentTime, int length) {
        ArrayList<Data> results = new ArrayList<Data>();
        Data d = streams.peek();
        while (d != null && d.arrivalTime > currentTime
                && d.arrivalTime <= currentTime + length) {
            results.add(d);
            streams.poll();
            d = streams.peek();

        }
        return results;

    }

    public void getRandomInput(int length, int range) {

        Random r = new Random();
        for (int i = 1; i <= length; i++) {
            double d = r.nextInt(range);
            Data data = new Data(d);
            data.arrivalTime = i;
            streams.add(data);

        }

    }

    public void getData(String filename) {

        try {
            BufferedReader bfr = new BufferedReader(new FileReader(new File(filename)));

            String line = "";
            int time = 1;
            try {
                while ((line = bfr.readLine()) != null) {

                    String[] atts = line.split(",");
                    double[] d = new double[atts.length];
                    for (int i = 0; i < d.length; i++) {

                        d[i] = Double.valueOf(atts[i]) + (new Random()).nextDouble() / 10000000;
                    }
                    Data data = new Data(d);
                    data.arrivalTime = time;
                    streams.add(data);
                    time++;
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public Stream() {
        Comparator<Data> comparator = new DataComparator();

       streams = new PriorityQueue<Data>(comparator);
    }

}

class DataComparator implements Comparator<Data> {

    @Override
    public int compare(Data x, Data y) {
        if (x.arrivalTime < y.arrivalTime) {
            return -1;
        } else if (x.arrivalTime > y.arrivalTime) {
            return 1;
        } else {
            return 0;
        }

    }

}
