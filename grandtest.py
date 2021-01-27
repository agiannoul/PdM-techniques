from grand import IndividualAnomalyInductive, IndividualAnomalyTransductive, GroupAnomaly
from grand.datasets import load_vehicles

import grand
print("VERSION:", grand.__version__)




if __name__ == "__main__":

    dataset = load_vehicles().normalize(with_std=False, with_mean=False)
    
    nb_units = dataset.get_nb_units()  # Number of systems (vehicles)
    ids_target_units = [0,1,2,3,4,7,8,9,11,12,13,14] #ids of busses where used in our case
    model = GroupAnomaly(nb_units, ids_target_units,w_martingale=15, w_ref_group="7days",dev_threshold=.5, non_conformity="median",metacheck=False,k=10,R=1,thresOut=5,thresIn=30
                         )
    for dt, x_units in dataset.stream():
        infos = model.predict(dt, x_units)
        print("Time: {} ==> {}".format(dt, infos[0]), end="\n", flush=True)
        
    
    
    model.plot_deviations(figsize=(13, 5), plots=["threshold","deviation"])
