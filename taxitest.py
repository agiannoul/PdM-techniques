import scipy.io
import numpy as np
import pandas as pd
import datetime
from grand import IndividualAnomalyInductive, IndividualAnomalyTransductive

df = pd.read_csv("nyc_taxi.csv")
for meas in ["lof"]:
    for size in [50]:
        Refsize=size
        non_conformity=meas
        k=20
        metacheck=False
        dev_threshold=0.6
        R=4000
        thresOut=1
        thresIn=30
        
        model = IndividualAnomalyInductive(w_martingale=15,non_conformity = non_conformity,k=k,metacheck=metacheck,dev_threshold=dev_threshold,R=R,thresOut=thresOut,thresIn=thresIn)
        #model =IndividualAnomalyTransductive(non_conformity = meas,k=10,ref_group = ["hour-of-day","day-of-week"], metacheck=metacheck,dev_threshold=dev_threshold,R=R,thresOut=thresOut,thresIn=thresIn,w_martingale = 100)
        
        #x = dffs[uid_test].loc[dt].values
        #Xref += list( dff[dt - pd.to_timedelta(self.w_ref_group) : dt].values )
        Tp=0
        Fp=0
        Fn=0
        Tn=0
        
        count=0
        outliers=[]
        outliers.append(datetime.datetime.strptime("2014-11-02 12:00:00", '%Y-%m-%d %H:%M:%S'))
        outliers.append(datetime.datetime.strptime("2014-11-27 12:00:00", '%Y-%m-%d %H:%M:%S'))
        outliers.append(datetime.datetime.strptime("2014-12-25 12:00:00", '%Y-%m-%d %H:%M:%S'))
        #outliers.append(datetime.datetime.strptime("2014-12-31 12:00:00", '%Y-%m-%d %H:%M:%S'))
        outliers.append(datetime.datetime.strptime("2015-01-01 12:00:00", '%Y-%m-%d %H:%M:%S'))
        #outliers.append(datetime.datetime.strptime("2015-01-25 12:00:00", '%Y-%m-%d %H:%M:%S'))
        
        Judostormdates=[]
        Judostormdates.append(datetime.datetime.strptime("2015-01-23 12:00:00", '%Y-%m-%d %H:%M:%S'))
        Judostormdates.append(datetime.datetime.strptime("2015-01-24 12:00:00", '%Y-%m-%d %H:%M:%S'))
        Judostormdates.append(datetime.datetime.strptime("2015-01-25 12:00:00", '%Y-%m-%d %H:%M:%S'))
        Judostormdates.append(datetime.datetime.strptime("2015-01-26 12:00:00", '%Y-%m-%d %H:%M:%S'))
        Judostormdates.append(datetime.datetime.strptime("2015-01-27 12:00:00", '%Y-%m-%d %H:%M:%S'))
        foundoutliers =dict()
        fp=dict()
        Infos=[]
        JudoStotm=False
        for dt,x in zip(df['timestamp'],df['value']):
            if count<Refsize:
                count+=1
                continue
            Xref=[]
            Temp=[]
            Temp += list( df['value'][max(count-Refsize,0): count])
            for i in Temp:
                Xref.append(np.array([i]))
            count+=1
            #If IndividualAnomalyInductive is used , then call model.fit() else comment it.
            model.fit(np.array(Xref))
            dt=datetime.datetime.strptime(dt, '%Y-%m-%d %H:%M:%S')
            info = model.predict(dt, np.array([x]))
            #print(str(dt)+" "+str(info))
            Infos.append(info)
            
            
            
            if info[3]==True:
                istp=False
                for out in outliers:
                    if out.date()==dt.date():
                        istp=True
                        foundoutliers[str(dt.date())]=1
                for out in Judostormdates:
                    if out.date()==dt.date():
                        JudoStotm=True
                        istp=True
                if istp==False:
                    fp[str(dt.date())]=1
        
        if JudoStotm:
            foundoutliers["2015-01-26"]=1
        plotoutliers=[]
        for f in outliers:
            plotoutliers.append(f)
        for f in Judostormdates:
            plotoutliers.append(f)
        model.plot_deviations(figsize=(12, 8), plots=["strangeness", "deviation", "threshold"],outl=plotoutliers,showdots=True)
        acc=str(len(foundoutliers)/(1.0*len(foundoutliers)+len(fp)))
        print("Acc = "+acc)
        recall=str(1.0*len(foundoutliers)/(len(outliers)+1))
        print("Recall = "+recall)

# =============================================================================
#         with open("C:\\Users\\panos\\Desktop\\matfiles\\taxi\\info.txt", "a") as myfile:
#             myfile.write("\n\n TEST: w mart")
#             myfile.write("\nPr size: "+str(Refsize))
#             myfile.write("\nMeasure: "+str(non_conformity))
#             if non_conformity!="median":
#                 myfile.write("\nk: "+str(k))
#             myfile.write("\nThreshold: "+str(dev_threshold))
#             myfile.write("\nMeta check: "+str(metacheck))
#             if metacheck==True:
#                 myfile.write("\nR: "+str(R))
#                 myfile.write("\nTin: "+str(thresIn))
#                 myfile.write("\nTout: "+str(thresOut))
#             myfile.write("\nAccurasy: "+str(acc))
#             myfile.write("\nRecall: "+str(recall))
# 
# =============================================================================

