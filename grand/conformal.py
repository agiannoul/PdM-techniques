"""Provide functions for conformal anomaly detection.
This module implements functions to compute non-conformity measures, p-values,
and tests for the uniformity of p-values.
"""

__author__ = "Mohamed-Rafik Bouguelia"
__license__ = "MIT"
__email__ = "mohamed-rafik.bouguelia@hh.se"

from grand import utils
from sklearn.neighbors import LocalOutlierFactor
import numpy as np
from sklearn.neighbors import KDTree
from grand.utils import NoRefGroupError
from scipy.stats import norm
from math import sqrt


def get_strangeness(measure="median", k=10):
    utils.validate_measure_str(measure)
    if measure == "median":
        return StrangenessMedian()
    elif measure == "knn":
        return StrangenessKNN(k)
    elif measure == "Baseline":
        return StrangenessBaseline()
    else:
        return StrangenessLOF(k)


class Strangeness:
    '''Class that wraps StrangenessMedian and StrangenessKNN and StrangenessLOF'''

    def __init__(self):
        self.X = None
        self.scores = None

    def is_fitted(self):
        return self.X is not None

    def fit(self, X):
        utils.validate_fit_input(X)
        self.X = X

    def predict(self, x):
        utils.validate_is_fitted(self.is_fitted())
        utils.validate_get_input(x)

    def pvalue(self, score):
        utils.validate_is_fitted(self.is_fitted())
        utils.validate_list_not_empty(self.scores)
        return len([1 for v in self.scores if v > score]) / len(self.scores)


class StrangenessMedian(Strangeness):
    '''Strangeness based on the distance to the median data (or most central pattern)'''

    def __init__(self):
        super().__init__()

    def fit(self, X):
        super().fit(X)
        self.med = np.median(X, axis=0)
        self.std = np.std(self.X)
        self.scores = [self.predictinner(x)[0] for x in self.X]
    def predict(self, x):
        super().predict(x)
        diff = x - self.med
        dist = np.linalg.norm(diff)
        return dist, diff, self.med
    def predictinner(self, x):
        super().predict(x)
        diff = x - self.med
        dist = np.linalg.norm(diff)
        return dist, diff, self.med 


class StrangenessKNN(Strangeness):
    '''Strangeness based on the average distance to the k nearest neighbors'''

    def __init__(self, k=10):
        super().__init__()
        self.k = k

    def fit(self, X):
        super().fit(X)
        X=np.array(X)
        self.kdt = KDTree(X, leaf_size=30, metric='euclidean')
        meansAr=[]
        if len(X)<=self.k:
           raise NoRefGroupError(" reference group data with less than k samples.")
        dists,self.idsall=self.kdt.query(X, k=self.k+1, return_distance=True)
        for i in range(len(X)):
            mean_knn_dists = np.mean(dists[i,1:])
            meansAr.append(mean_knn_dists)
        self.scores=meansAr 
        #self.scores = [self.predict(xx)[0] for xx in self.X]

    def predict(self, x):
        super().predict(x)
        dists = np.array([np.linalg.norm(x - xx) for xx in self.X])
        ids = np.argsort(dists)
        ids = ids[1:self.k+1] if np.array_equal(x, self.X[ids[0]]) else ids[:self.k]

        mean_knn_dists = np.mean(dists[ids])
        #print("\n MESOS OROS : "+str(mean_knn_dists)+"\n")
        representative = np.mean(np.array(self.X)[ids], axis=0)
        diff = x - representative
        self.nn=0
        #for xx in self.X:
        #    if np.linalg.norm(x - xx)<1 :
        #        self.nn +=1
        return mean_knn_dists, diff, representative 

        


class StrangenessLOF(Strangeness):
    '''Strangeness based on the local outlier factor (LOF)'''
    def __init__(self, k=10):
        super().__init__()
        utils.validate_int_higher(k, 0)
        self.k = k
        self.lof = LocalOutlierFactor(n_neighbors=k, novelty=True, contamination="auto")

    def fit(self, X):
        super().fit(X)
        X_ = list(X) + [ X[-1] for _ in range(self.k - len(X)) ]
        self.lof.fit(X_)
        self.scores = -1 * self.lof.negative_outlier_factor_

    def predict(self, x):
        super().predict(x)
        outlier_score = -1 * self.lof.score_samples([x])[0]
        med = np.median(self.X, axis=0) # FIXME: temporary hack
        diff = x - med
        return outlier_score, diff, med

class StrangenessBaseline(Strangeness):
    '''Histogram based method'''
    def __init__(self, k=10):
        super().__init__()
    def fit(self, X):
        super().fit(X)
        self.n=len(X)
    def predict(self, x):
        D , c = self.CalculateD()
        score=0
        dmc=self.HellingerDist(x,self.X[c])
        for i in range(len(D)):
            if D[i][c] > dmc:
                score+=1
        score=score/len(D)
        return score
    def CalculateD(self):
        D=[]
        central=-1
        centralsum=-1
        n=len(self.X)
        
        for intexr in range(n):
            D.append([])
            summ=0
            for indexc in range(intexr):
                D[intexr].append(D[indexc][intexr])
                summ+=D[indexc][intexr]
            for indexc in range(intexr,n):
                tempdist=self.HellingerDist(self.X[intexr],self.X[indexc])
                D[intexr].append(tempdist)
                summ+=tempdist
            if centralsum>summ:
                central=intexr
                centralsum=summ
        return D , central
    
    def HellingerDist(self,a,b):
        summ=0
        for i in range(len(a)):
            q=sqrt(a[i])-sqrt(b[i])
            summ+=q*q
        return sqrt(0.5*summ)