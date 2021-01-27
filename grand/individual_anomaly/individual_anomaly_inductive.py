"""An inductive anomaly detection model for an individual system.
To detect anomalies, the IndividualAnomalyInductive model compares the data from
a system against a fixed reference dataset.
"""

__author__ = "Mohamed-Rafik Bouguelia"
__license__ = "MIT"
__email__ = "mohamed-rafik.bouguelia@hh.se"

from grand.conformal import get_strangeness
from grand.utils import DeviationContext, append_to_df
from grand import utils
import matplotlib.pylab as plt, pandas as pd, numpy as np
from pandas.plotting import register_matplotlib_converters
from scipy.stats import norm
from math import sqrt

class IndividualAnomalyInductive:
    '''Deviation detection for a single/individual unit
    
    Parameters:
    ----------
    w_martingale : int
        Window used to compute the deviation level based on the last w_martingale samples. 
                
    non_conformity : string
        Strangeness (or non-conformity) measure used to compute the deviation level.
        It must be either "median" or "knn"
                
    k : int
        Parameter used for k-nearest neighbours, when non_conformity is set to "knn"
        
    dev_threshold : float
        Threshold in [0,1] on the deviation level
    '''
    
    def __init__(self, w_martingale=15, non_conformity="median", k=20, dev_threshold=0.6, columns=None,R=1.5,thresOut=5,thresIn=30,metacheck=False):
        utils.validate_individual_deviation_params(w_martingale, non_conformity, k, dev_threshold)
        self.metacheck=metacheck
        self.thresOut=thresOut
        self.thresIn=thresIn
        self.k = k
        self.R=R
        self.w_martingale = w_martingale
        self.non_conformity = non_conformity

        self.dev_threshold = dev_threshold
        self.columns = columns
        self.strg = get_strangeness(non_conformity, k)
        self.T, self.S, self.P, self.M , self.isDev, self.NN = [], [], [], [] ,[],[]
        self.representatives, self.diffs = [], []
        
        self.mart = 0
        self.marts = [0, 0, 0]
        self.df = pd.DataFrame(data=[], index=[])

    # ===========================================
    def fit(self, X):
        '''Fit the anomaly detector to the data X (assumed to be normal)
        Parameters:
        -----------
        X : array-like, shape (n_samples, n_features)
            Samples assumed to be not deviating from normal
        
        Returns:
        --------
        self : object
        '''
        self.X=X
        self.strg.fit(X)
        return self
    
    # ===========================================
    '''Update the deviation level based on the new test sample x
        
        Parameters:
        -----------
        dtime : datetime
            datetime corresponding to the sample x
        
        x : array-like, shape (n_features,)
            Sample for which the strangeness, p-value and deviation level are computed
        
        Returns:
        --------
        strangeness : float
            Strangeness of x with respect to samples in Xref
        
        pval : float, in [0, 1]
            p-value that represents the proportion of samples in Xref that are stranger than x.
        
        deviation : float, in [0, 1]
            Normalized deviation level updated based on the last w_martingale steps
        '''
    def predict(self, dtime, x):
    
        
        self.T.append(dtime)
        self.df = append_to_df(self.df, dtime, x)
        
        #Histogram based ( Basiline method using normal distribution)
        if self.non_conformity=="Baseline":
            strangeness = self.strg.predict(x)
            deviation = self._update_z_score(strangeness)
            self.P.append(deviation)
            is_deviating = deviation < 0.05
            self.M.append(deviation)
        
            if is_deviating:
                self.isDev.append(1)
            else:
                self.isDev.append(0)
            return DeviationContext(strangeness, 0,deviation, is_deviating)
        strangeness, diff, representative= self.strg.predict(x)
        
        self.S.append(strangeness)
        self.diffs.append(diff)
        self.representatives.append(representative)
        
        pval = self.strg.pvalue(strangeness)
        self.P.append(pval)
        
        deviation = self._update_martingale(pval)
        self.M.append(deviation)
        
        is_deviating = deviation > self.dev_threshold
        #Hybrid solution applying metacheck
        if self.metacheck==False:
            if is_deviating:
                self.isDev.append(1)
            else:
                self.isDev.append(0)
        else:
            nn=self.calculate_nn(x,self.X)
            self.NN.append(nn)
            if is_deviating==False :
                if  nn<self.thresOut:
                    self.isDev.append(2)
                    is_deviating = True
                else:
                    self.isDev.append(0)
            else:
                if nn > self.thresIn :
                    is_deviating=False
                    self.isDev.append(-1)
                else :
                    self.isDev.append(1)
        return DeviationContext(strangeness, pval, deviation, is_deviating)
    
    
    # ===========================================
    def calculate_nn(self,x,X):
        nn=0
        for xx in X:
           if np.linalg.norm(x - xx)<self.R :
                nn +=1
        return nn
    def _update_z_score(self, score):
        self.mart += score
        self.marts.append(self.mart)
        w = min(self.w_martingale, len(self.marts))
        mat_in_window = self.mart - self.marts[-w]
        normalized_mart = mat_in_window / w
        return norm.cdf(normalized_mart,0.5,sqrt(1.0/(12*w)))
        
    # ===========================================
    def _update_martingale(self, pval):
        '''Incremental additive martingale over the last w_martingale steps.
        
        Parameters:
        -----------
        pval : int, in [0, 1]
            The most recent p-value
        
        Returns:
        --------
        normalized_one_sided_mart : float, in [0, 1]
            Deviation level. A normalized version of the current martingale value.
        '''
        
        betting = lambda p: -p + .5
        
        self.mart += betting(pval)
        self.marts.append(self.mart)
        
        w = min(self.w_martingale, len(self.marts))#pare to mikrotero apo W kai ta stoixeia pou exoun perasei
        mat_in_window = self.mart - self.marts[-w]
        #mat_in_winodw has the sum of betting in current window
        normalized_mart = ( mat_in_window ) / (.5 * w)
        normalized_one_sided_mart = max(normalized_mart, 0)
        return normalized_one_sided_mart
        
    # ===========================================
    def get_stats(self):
        stats = np.array([self.S, self.M, self.P]).T
        return pd.DataFrame(index=self.T, data=stats, columns=["strangeness", "deviation", "pvalue"])

    # ===========================================
    def get_all_deviations(self, min_len=5, dev_threshold=None):
        if dev_threshold is None: dev_threshold = self.dev_threshold

        arr = np.arange(len(self.M))
        boo = np.array(self.M) > dev_threshold
        indices = np.nonzero(boo[1:] != boo[:-1])[0] + 1
        groups_ids = np.split(arr, indices)
        groups_ids = groups_ids[0::2] if boo[0] else groups_ids[1::2]

        diffs_df = pd.DataFrame(index=self.T, data=self.diffs)
        sub_diffs_dfs = [diffs_df.iloc[ids, :] for ids in groups_ids if len(ids) >= min_len]

        dev_signatures = [np.mean(sub_diffs_df.values, axis=0) for sub_diffs_df in sub_diffs_dfs]
        periods = [(sub_diffs_df.index[0], sub_diffs_df.index[-1]) for sub_diffs_df in sub_diffs_dfs]

        deviations = [(devsig, p_from, p_to) for (devsig, (p_from, p_to)) in zip(dev_signatures, periods)]
        return deviations

    # ===========================================
    def get_deviation_signature(self, from_time, to_time):
        sub_diffs_df = pd.DataFrame(index=self.T, data=self.diffs)[from_time: to_time]
        deviation_signature = np.mean(sub_diffs_df.values, axis=0)
        return deviation_signature

    # ===========================================
    def get_similar_deviations(self, from_time, to_time, k_devs=2, min_len=5, dev_threshold=None):
        target_devsig = self.get_deviation_signature(from_time, to_time)
        deviations = self.get_all_deviations(min_len, dev_threshold)
        dists = [np.linalg.norm(target_devsig - devsig) for (devsig, *_) in deviations]
        ids = np.argsort(dists)[:k_devs]
        return [deviations[id] for id in ids]

    # ===========================================
    def plot_deviations(self, figsize=None, savefig=None, plots=["data", "strangeness", "pvalue", "deviation", "threshold"],outl=[], debug=False,showdots=False):
        '''Plots the anomaly score, deviation level and p-value, over time.'''

        register_matplotlib_converters()

        plots, nb_axs, i = list(set(plots)), 0, 0
        if "data" in plots:
            nb_axs += 1
        if "strangeness" in plots:
            nb_axs += 1FalF
        if any(s in ["pvalue", "deviation", "threshold"] for s in plots):
            nb_axs += 1

        fig, axes = plt.subplots(nb_axs, sharex="row", figsize=figsize)
        if not isinstance(axes, (np.ndarray) ):
            axes = np.array([axes])

        if "data" in plots:
            axes[i].set_xlabel("Time")
            axes[i].set_ylabel("Feature 0")
            axes[i].plot(self.df.index, self.df.values[:, 0], label="Data")
            if debug:
                axes[i].plot(self.T, np.array(self.representatives)[:, 0], label="Reference")
            axes[i].legend()
            i += 1

        if "strangeness" in plots:
            axes[i].set_xlabel("Time")
            axes[i].set_ylabel("Strangeness")
            axes[i].plot(self.T, self.S, label="Strangeness")
            #axes[i].plot(self.T, self.NN, color='r',label="Nn")
            if debug:
                axes[i].plot(self.T, np.array(self.diffs)[:, 0], label="Difference")
            axes[i].legend()
            i += 1

        if any(s in ["pvalue", "deviation", "threshold"] for s in plots):
            axes[i].set_xlabel("Time")
            axes[i].set_ylabel("Deviation")
            axes[i].set_ylim(0, 1)
            if "pvalue" in plots:
                axes[i].scatter(self.T, self.P, alpha=0.25, marker=".", color="green", label="p-value")
            if "deviation" in plots:
                axes[i].plot(self.T, self.M, label="Deviation")
                plotdots=showdots
                if plotdots:
                    ddd=[]
                    dd=[]
                    dddd=[]
                    datesout=[]
                    for dt , isd in zip(self.T,self.isDev):
                        if isd==1 :
                            ddd.append(dt)
                            datesout.append(dt)
                        elif isd==2:
                            dd.append(dt)
                            datesout.append(dt)
                        elif isd==-1:
                            dddd.append(dt)
                            datesout.append(dt)
                    if self.non_conformity=="Baseline":
                        axes[i].plot(ddd, [0.05 for ddt in ddd],"bo")
                    else:
                        axes[i].plot(ddd, [0.5 for ddt in ddd],"bo")
                    axes[i].plot(dd, [0.5 for ddt in dd],"mo")
                    axes[i].plot(dddd, [0.5 for ddt in dddd],"go")
                for out in outl:
                    axes[i].axvline(out , color='r')
            if "threshold" in plots:
                axes[i].axhline(y=self.dev_threshold, color='r', linestyle='--', label="Threshold")
            axes[i].legend()

        fig.autofmt_xdate()

        if savefig is None:
            plt.draw()
            plt.show()
        else:
            figpathname = utils.create_directory_from_path(savefig)
            plt.savefig(figpathname)

    # ===========================================
    def plot_explanations(self, from_time, to_time, figsize=None, savefig=None, k_features=4):
        # TODO: validate if the period (from_time, to_time) has data before plotting

        from_time_pad = from_time - (to_time - from_time)
        to_time_pad = to_time + (to_time - from_time)

        sub_df = self.df[from_time: to_time]
        sub_df_before = self.df[from_time_pad: from_time]
        sub_df_after = self.df[to_time: to_time_pad]

        sub_representatives_df_pad = pd.DataFrame(index=self.T, data=self.representatives)[from_time_pad: to_time_pad]
        sub_diffs_df = pd.DataFrame(index=self.T, data=self.diffs)[from_time: to_time]

        nb_features = sub_diffs_df.values.shape[1]
        if (self.columns is None) or (len(self.columns) != nb_features):
            self.columns = ["Feature {}".format(j) for j in range(nb_features)]
        self.columns = np.array(self.columns)

        features_scores = np.array([np.abs(col).mean() for col in sub_diffs_df.values.T])
        features_scores = 100 * features_scores / features_scores.sum()
        k_features = min(k_features, nb_features)
        selected_features_ids = np.argsort(features_scores)[-k_features:][::-1]
        selected_features_names = self.columns[selected_features_ids]
        selected_features_scores = features_scores[selected_features_ids]

        fig, axs = plt.subplots(k_features, figsize=figsize)
        fig.autofmt_xdate()
        fig.suptitle("Ranked features\nFrom {} to {}".format(from_time, to_time))
        if k_features == 1 or not isinstance(axs, (np.ndarray) ):
            axs = np.array([axs])

        for i, (j, name, score) in enumerate(zip(selected_features_ids, selected_features_names, selected_features_scores)):
            print("{0}, score: {1:.2f}%".format(name, score))
            axs[i].set_xlabel("Time")
            axs[i].set_ylabel("{0}\n(Score: {1:.1f})".format(name, score))
            axs[i].plot(sub_representatives_df_pad.index, sub_representatives_df_pad.values[:, j], color="grey", linestyle='--')
            axs[i].plot(sub_df_before.index, sub_df_before.values[:, j], color="green")
            axs[i].plot(sub_df_after.index, sub_df_after.values[:, j], color="lime")
            axs[i].plot(sub_df.index, sub_df.values[:, j], color="red")

        figg = None
        if k_features > 1:
            figg, ax1 = plt.subplots(figsize=figsize)
            figg.suptitle("Top 2 ranked features\nFrom {} to {}".format(from_time, to_time))
            (j1, j2) , (nm1, nm2), (s1, s2) = selected_features_ids[:2], selected_features_names[:2], selected_features_scores[:2]

            ax1.set_xlabel("{0}\n(Score: {1:.1f})".format(nm1, s1))
            ax1.set_ylabel("{0}\n(Score: {1:.1f})".format(nm2, s2))

            self.strg.X = np.array(self.strg.X)
            ax1.scatter(self.strg.X[:, j1], self.strg.X[:, j2], color="silver", marker=".", label="Reference Data")
            ax1.scatter(sub_df_before.values[:, j1], sub_df_before.values[:, j2], color="green", marker=".", label="Before Selected Period")
            ax1.scatter(sub_df_after.values[:, j1], sub_df_after.values[:, j2], color="lime", marker=".", label="After Selected Period")
            ax1.scatter(sub_df.values[:, j1], sub_df.values[:, j2], color="red", marker=".", label="Selected Period")
            ax1.legend()

        if savefig is None:
            plt.show()
        else:
            figpathname = utils.create_directory_from_path(savefig)
            fig.savefig(figpathname)
            if figg is not None: figg.savefig(figpathname + "_2.png")
