# PdM-techniques


Java implementaion of MCOD from https://infolab.usc.edu/Luan/Outlier/

  For testing busses data use MTTestBus.java while for testing the other dataset use MTTest.java.
  Use SetArguments() to set the parameters and the correct path to the datafile.
  
  In case of Bus testing, in MTTestBus.java set the Constant.datafile to the path of the folder which contains the csv files of busses.
  This folder may contain only these datafiles (i.e. 19 csv file with bus data)!
  
  For testing cpulatancy.csv , mahine_temperature.csv , ambient_temperature.csv and nyc_taxi.csv use MTTest.java and in the end of main call
  tpfpCpuLatncy(); , tpfpMachineTemp(); ,  tpfpambientTemp(); or tofoTaxi(); (only one of them) depending on the datafile which is tested to evaluate the results.


Grand method from https://github.com/caisr-hh/group-anomaly-detection with Hybrid solution extention.
