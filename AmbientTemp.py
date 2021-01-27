import scipy.io
import numpy as np
import pandas as pd
import datetime
from datetime import timedelta
from grand import IndividualAnomalyInductive, IndividualAnomalyTransductive

df = pd.read_csv("ambient_temperature.csv")
for meas in ["lof"]:
    for size in [300]:
        Refsize=size
        non_conformity=meas
        k=10
        phours=8
        metacheck=False
        dev_threshold=0.4
        R=3
        thresOut=10
        thresIn=20
        
        model = IndividualAnomalyInductive(w_martingale=50,non_conformity = non_conformity,k=k,metacheck=metacheck,dev_threshold=dev_threshold,R=R,thresOut=thresOut,thresIn=thresIn)
        #model=IndividualAnomalyTransductive(non_conformity = meas,k=10,ref_group = ["hour-of-day"], w_martingale = 50)
        
        #x = dffs[uid_test].loc[dt].values
        #Xref += list( dff[dt - pd.to_timedelta(self.w_ref_group) : dt].values )
        Tp=0
        Fp=0
        Fn=0
        Tn=0                        
        count=0
        outliers=[datetime.datetime.strptime("2013-12-22 20:00:00", '%Y-%m-%d %H:%M:%S'),datetime.datetime.strptime("2014-04-13 09:00:00", '%Y-%m-%d %H:%M:%S')]
        fountoutliers=dict()
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
            model.fit(np.array(Xref))
            dt=datetime.datetime.strptime(dt, '%Y-%m-%d %H:%M:%S')
            info = model.predict(dt, np.array([x]))
            if info[3]:
                istp=False
                for out in outliers:
                    if dt<=out and dt>=out - timedelta(hours=phours):
                        istp=True
                        fountoutliers[str(out)]=1
                if istp:
                    Tp+=1
                else:
                    Fp+=1
            print(str(dt)+" "+str(info))

        model.plot_deviations(figsize=(12, 8), plots=[ "deviation", "threshold"],outl=outliers,showdots=True)
        acc=1.0*Tp/(Tp+Fp)
        print("Acc = "+str(acc))
        recall=1.0*len(fountoutliers)/len(outliers)
        print("Recall = "+str(recall))
        F1=0
        if acc!=0 and recall!=0:
            F1=2/((1/acc)+(1/recall))
        print("F1 = "+str(F1))
# =============================================================================
#         with open("C:\\Users\\panos\\Desktop\\matfiles\\ambient_temperature\\info.txt", "a") as myfile:
#             myfile.write("\n\n TEST:")
#             myfile.write("\nPr size: "+str(Refsize))
#             myfile.write("\nMeasure: "+str(non_conformity))
#             if non_conformity!="median":
#                 myfile.write("\nk: "+str(k))
#             myfile.write("\nThreshold: "+str(dev_threshold))
#             myfile.write("\nPh hours: "+str(phours))
#             myfile.write("\nMeta check: "+str(metacheck))
#             if metacheck==True:
#                 myfile.write("\nR: "+str(R))
#                 myfile.write("\nTin: "+str(thresIn))
#                 myfile.write("\nTout: "+str(thresOut))
#             myfile.write("\nAccurasy: "+str(acc))
#             myfile.write("\nRecall: "+str(recall))
#             myfile.write("\nF1: "+str(F1))
# =============================================================================

