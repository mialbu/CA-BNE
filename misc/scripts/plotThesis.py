import os
import sys
import glob
import re
import math

import matplotlib as mpl

import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as anim

import itertools
import time

def calcMaxRelUtilityGains(folder : str) -> []:
    maxRelUtilityGains = []
    for i in range(400):
        if i <10:
            file = "00" + str(i)
        elif i <100:
            file = "0" + str(i)
        else:
            file = str(i)

        sStrategyDict = {}
        oStrategyDict = {}

        with open (folder + "%s.finalSimpleStrategies" % file) as fd:
            first = True
            nr = 0
            itr = 0
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                strategy = line.split("  ")[1:-1]
                sStrategyDict[int(bidder)] = strategy
        with open (folder + "%s.finalOverbidStrategies" % file) as fd:
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                oStrategy = line.split("  ")[1:]
                oStrategyDict[int(bidder)] = oStrategy
        for b in oStrategyDict:
            maxRelUtilityGain = 0
            strat = sStrategyDict[b]
            oStrat = oStrategyDict[b]
            for point in range(len(strat)):
                valuation = float(strat[point].split(" ")[0])
                if (valuation != float(oStrat[point].split(" ")[0])):
                    print("wrong comparison")
                else:
                    sUtility = float(strat[point].split(" ")[1])
                    oUtility = float(oStrat[point].split(" ")[1])
                    if (sUtility == 0.0):
                        relUtilityGain = 0.0
                    else:
                        relUtilityGain = (oUtility / sUtility) - 1.0
                    if (relUtilityGain > maxRelUtilityGain):
                        maxRelUtilityGain = relUtilityGain
            maxRelUtilityGains.append(maxRelUtilityGain)
    return  maxRelUtilityGains

def boxPlotRelUtilities(maxRelUtilityGains: [], maxRelUtilityGains15: []):
    dict = {}
    dict[0.0] = 0
    for i in range(300):
        p_low = float(i/100)
        p_high = float((i+1)/100)
        count = 0
        for relUtil in maxRelUtilityGains:
            # print(relUtil)
            if ((relUtil > p_low) and (relUtil<p_high)):
                count+=1
        dict[p_high] = count/565
    # print(dict)
    box_x = []
    box_y = []
    for x in dict:
        if (dict[x] != 0.0):
            box_x.append(x)
            box_y.append(dict[x])


    # for t in range(len(box_x)):
    #     x = box_x[t]
    #     y = box_y[t]
    #     plt.text(x,y, s=str(y), size=5)

    dict15 = {}
    dict15[0.0] = 0
    for i in range(300):
        p_low = float(i/100)
        p_high = float((i+1)/100)
        count = 0
        for relUtil in maxRelUtilityGains15:
            # print(relUtil)
            if ((relUtil > p_low) and (relUtil<p_high)):
                count+=1
        dict15[p_high] = count/833
    # print(dict)
    box_x15 = []
    box_y15 = []
    for x in dict15:
        if (dict15[x] != 0.0):
            box_x15.append(x)
            box_y15.append(dict[x])


    plt.subplot(1,2,1)
    plt.setp(plt.stem(box_x, box_y, markerfmt='-'), color='b', linewidth=0.5)
    plt.title("8 Items")
    plt.xlabel("Maximal relative utility gain")
    plt.ylabel("Fraction of deviating bidders")

    plt.subplot(1,2,2)
    plt.setp(plt.stem(box_x15, box_y15, markerfmt='-'), color='b', linewidth=0.5)
    plt.title("15 Items")
    plt.xlabel("Maximal relative utility gain")
    plt.ylabel("Fraction of deviating bidders")
    plt.suptitle("Distribution Of Maximal Relative Utility Gains")

    plt.show()

def plotFinalOverbidStrategy(folder: str, file: str):
    with open(folder + "%s.finalOverbidStrategies" % file) as fd:
        strategy = fd.readline().rstrip().split("  ")[1:]
        valuation = []
        utility = []
        bids = []
        obids = []

        for t in strategy:
            valuation.append(float(t.split(" ")[0]))
            utility.append(float(t.split(" ")[1]))
            bids.append(float(t.split(" ")[2]))
            obids.append(float(t.split(" ")[3]))

    with open(folder + "%s.finalSimpleStrategies" % file) as fd:
        strategy = fd.readline().rstrip().split("  ")[1:]
        singleValuation = []
        singleUtility = []
        singleBids = []

        for t in strategy:
            singleValuation.append(float(t.split(" ")[0]))
            singleUtility.append(float(t.split(" ")[1]))
            singleBids.append(float(t.split(" ")[2]))


        # Strategy Plots
        plt.subplot(1, 2, 1)
        plt.plot(singleValuation, singleBids, color = 'g', clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label="Naive Bidding")
        plt.plot(valuation, bids, color = 'b', clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label="Bid On Original Bundle When Overbidding")
        plt.plot(valuation, obids, color = 'r', clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label="Bid On Second Bundle When Overbidding")
        plt.legend()
        plt.title('Strategies')
        plt.ylabel('Bid')
        plt.xlabel('Valuation')

        # Utility Plots
        plt.subplot(1, 2, 2)
        plt.plot(singleValuation, singleUtility, color = 'g', clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label="Naive Utility")
        plt.plot(valuation, utility, color = 'b', clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label="Utility When Overbidding")
        plt.legend()

        plt.title('Utility')
        plt.ylabel('Utility')
        plt.xlabel('Valuation')

        plt.show()

def plotOverbidStrategy(folder: str, file: str):
    with open(folder + "%s.overbiddingBRStrats" % file) as fd:
        fd.readline()
        fd.readline()
        fd.readline()
        fd.readline()
        fd.readline()
        strategy = fd.readline().rstrip().split("  ")
        valuation = []
        utility = []
        bids = []
        obids = []

        for t in strategy:
            valuation.append(float(t.split(" ")[0]))
            utility.append(float(t.split(" ")[1]))
            bids.append(float(t.split(" ")[2]))
            obids.append(float(t.split(" ")[3]))

        plt.plot(valuation, utility, color = 'g')
        plt.plot(valuation, bids, color = 'b')
        plt.plot(valuation, obids, color = 'r')
        plt.show()

def plotStrategy(folder: str, file: str):
    with open(folder + "%s.simpleBRstrats" % file) as fd:
        fd.readline()
        strategy = fd.readline().rstrip().split("  ")[1:]
        valuation = []
        utility = []
        bids = []

        for t in strategy:
            valuation.append(float(t.split(" ")[0]))
            utility.append(float(t.split(" ")[1]))
            bids.append(float(t.split(" ")[2]))

        plt.plot(valuation, utility, color = 'g')
        plt.plot(valuation, bids, color = 'b')
        plt.show()

def plotSingleStrategy(folder: str, file: str):
    with open(folder + "%s.eqStrats" % file) as fd:
        fd.readline()
        fd.readline()
        fd.readline()
        fd.readline()
        fd.readline()
        fd.readline()
        strategy = fd.readline().rstrip().split("  ")[1:]
        valuation = []
        bids = []

        for t in strategy:
            valuation.append(float(t.split(" ")[0]))
            bids.append(float(t.split(" ")[1]))

        plt.plot(valuation, bids, color = 'b')
        plt.show()

def plotUtility(folder : str, file: str):
    sUtilityDict = {}
    sBidDict = {}
    sUtil_x = []
    sUtil_y = []
    sBid_y = []
    with open (folder + "%s.finalSimpleStrategies" % file) as fd:
        line = fd.readline()
        array = line.split("  ")
        array = array[1:-1]
    for tr in array:
        triple = tr.split(" ")
        sUtilityDict[float(triple[0])] = float(triple[1])
        sBidDict[float(triple[0])] = float(triple[2])
    for i in sUtilityDict:
        sUtil_x.append(i)
        sUtil_y.append(sUtilityDict[i])
        sBid_y.append(sBidDict[i])

    oUtilityDict = {}
    oBidDict_1 = {}
    oBidDict_2 = {}
    oUtil_x = []
    oUtil_y = []
    oBid1_y = []
    oBid2_y = []
    with open (folder + "%s.finalOverbidStrategies" % file) as fd:
        line = fd.readline()
        array = line.split("  ")
        array = array[1:-1]
    for qr in array:
        quadruple = qr.split(" ")
        oUtilityDict[float(quadruple[0])] = float(quadruple[1])
        oBidDict_1[float(quadruple[0])] = float(quadruple[2])
        oBidDict_2[float(quadruple[0])] = float(quadruple[3])
    for i in oUtilityDict:
        oUtil_x.append(i)
        oUtil_y.append(oUtilityDict[i])
        oBid1_y.append(oBidDict_1[i])
        oBid2_y.append(oBidDict_2[i])

    plt.style.use('seaborn-whitegrid')
    fig = plt.figure()
    plt.plot(sUtil_x, sUtil_y, color = 'b')
    plt.plot(sUtil_x, sBid_y, '--', color = 'b')
    plt.plot(oUtil_x, oUtil_y, color = 'g')
    plt.plot(oUtil_x, oBid1_y, '--', color = 'g')
    plt.plot(oUtil_x, oBid2_y, '-.', color = 'g')
    plt.show()

def howManyTimesOverBidBelowBid(folder: str, nrOfFiles):
    nrTotalCompared = 0
    nrTotaloBidGreaterEqual = 0
    nrTotaloBidLower = 0
    nrTotalBidders = 0
    for i in range(nrOfFiles):
        if i<10:
            file = "00" + str(i)
        elif i<100:
            file = "0" + str(i)
        else:
            file = str(i)
        oBidGreaterEqualBid = 0
        oBidLowerThanBid = 0
        with open (folder + "%s.finalOverbidStrategies" % file) as fd:
            for line in fd.readlines():
                nrTotalBidders += 1
                bidder = line.split("  ")[0]
                oStrategy = line.split("  ")[1:]
                for j in range(len(oStrategy)):
                    nrTotalCompared+=1
                    bid = oStrategy[j].split(" ")[2]
                    oBid = oStrategy[j].split(" ")[3]
                    if (oBid>=bid):
                        oBidGreaterEqualBid+=1
                        nrTotaloBidGreaterEqual+=1
                    else:
                        oBidLowerThanBid+=1
                        nrTotaloBidLower+=1
    print("\nNr of all deviating Bidders: " +str(nrTotalBidders))
    print("Nr of compared value points: " + str(nrTotalCompared))
    print("Nr of deviating bids greater or equal to the true bid: " + str(nrTotaloBidGreaterEqual))
    print("Nr of deviating bids lower than the true bid: " + str(nrTotaloBidLower))

def calcHowManyTimesTheCases(folder:str, nrOfFiles:int):
    nrTotalBidders = 0
    nrTotalCompared = 0
    nrSingleHigher = 0
    nrSingleMiddleEqual = 0
    nrSingleLower = 0
    for i in range(nrOfFiles):
        if i<10:
            file = "00" + str(i)
        elif i<100:
            file = "0" + str(i)
        else:
            file = str(i)

        oStrategies = []
        sStrategies = []
        with open (folder + "%s.finalOverbidStrategies" % file) as fd:
            for line in fd.readlines():
                nrTotalBidders += 1
                bidder = line.split("  ")[0]
                oStrategies.append(line.rstrip().split("  ")[1:])
        with open (folder + "%s.finalSimpleStrategies" % file) as fd:
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                sStrategies.append(line.rstrip().split("  ")[1:])

        for strat in range(len(sStrategies)):
            for j in range(1001):
                singleBid = float(sStrategies[strat][j].split(" ")[2])
                trueBid = float(oStrategies[strat][j].split(" ")[2])
                oBid = float(oStrategies[strat][j].split(" ")[3])
                nrTotalCompared+=1

                #case 1
                if singleBid<trueBid and singleBid<oBid:
                    nrSingleLower+=1
                elif singleBid>trueBid and singleBid>oBid:
                    nrSingleHigher+=1
                else:
                    nrSingleMiddleEqual+=1
    print("Nr total compared bid points: " + str(nrTotalCompared))
    print("Nr single bid lower: " + str(nrSingleLower))
    print("Nr single bid higher: " + str(nrSingleHigher))
    print("Nr single bid in middle: " + str(nrSingleMiddleEqual))

def plotNumberAtWhichPercentageTheMaximalRelUtilityWas(folder:str, folder15:str, nrOfFiles:int):
    maxRelUtilityGainPercentageOfMaxValuation = []
    maxRelUtilityGainPercentageOfMaxValuation15 = []
    for i in range(nrOfFiles):
        if i<10:
            file = "00" + str(i)
        elif i<100:
            file = "0" + str(i)
        else:
            file = str(i)

        maxRelUtilityGains = []
        maxRelUtilityLosses = []


        sStrategyDict = {}
        oStrategyDict = {}

        with open (folder + "%s.finalSimpleStrategies" % file) as fd:
            first = True
            nr = 0
            itr = 0
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                strategy = line.split("  ")[1:-1]
                sStrategyDict[int(bidder)] = strategy
        with open (folder + "%s.finalOverbidStrategies" % file) as fd:
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                oStrategy = line.split("  ")[1:]
                oStrategyDict[int(bidder)] = oStrategy

        for b in oStrategyDict:
            maxRelUtilityGain = 0.0
            maxRelUtilityLoss = 0.0

            strat = sStrategyDict[b]
            oStrat = oStrategyDict[b]
            for point in range(len(strat)):
                valuation = float(strat[point].split(" ")[0])
                if (valuation != float(oStrat[point].split(" ")[0])):
                    print("wrong comparison")
                else:
                    sUtility = float(strat[point].split(" ")[1])
                    oUtility = float(oStrat[point].split(" ")[1])
                    if (sUtility == 0.0):
                        relUtilityGain = 0.0
                    else:
                        relUtilityGain = (oUtility / sUtility) - 1.0
                    if (relUtilityGain > maxRelUtilityGain):
                        maxRelUtilityGain = relUtilityGain
                        maxAtValuation = point/(len(strat)-1)
                        if (relUtilityGain < maxRelUtilityLoss):
                            maxRelUtilityLoss = relUtilityGain
            maxRelUtilityGains.append(maxRelUtilityGain)
            maxRelUtilityLosses.append(maxRelUtilityLoss)
            maxRelUtilityGainPercentageOfMaxValuation.append(maxAtValuation)

    for i in range(nrOfFiles):
        if i<10:
            file = "00" + str(i)
        elif i<100:
            file = "0" + str(i)
        else:
            file = str(i)

        maxRelUtilityGains = []
        maxRelUtilityLosses = []


        sStrategyDict = {}
        oStrategyDict = {}

        with open (folder15 + "%s.finalSimpleStrategies" % file) as fd:
            first = True
            nr = 0
            itr = 0
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                strategy = line.split("  ")[1:-1]
                sStrategyDict[int(bidder)] = strategy
        with open (folder15 + "%s.finalOverbidStrategies" % file) as fd:
            for line in fd.readlines():
                bidder = line.split("  ")[0]
                oStrategy = line.split("  ")[1:]
                oStrategyDict[int(bidder)] = oStrategy

        for b in oStrategyDict:
            maxRelUtilityGain = 0.0
            maxRelUtilityLoss = 0.0

            strat = sStrategyDict[b]
            oStrat = oStrategyDict[b]
            for point in range(len(strat)):
                valuation = float(strat[point].split(" ")[0])
                if (valuation != float(oStrat[point].split(" ")[0])):
                    print("wrong comparison")
                else:
                    sUtility = float(strat[point].split(" ")[1])
                    oUtility = float(oStrat[point].split(" ")[1])
                    if (sUtility == 0.0):
                        relUtilityGain = 0.0
                    else:
                        relUtilityGain = (oUtility / sUtility) - 1.0
                    if (relUtilityGain > maxRelUtilityGain):
                        maxRelUtilityGain = relUtilityGain
                        maxAtValuation = point/(len(strat)-1)
                        if (relUtilityGain < maxRelUtilityLoss):
                            maxRelUtilityLoss = relUtilityGain
            maxRelUtilityGains.append(maxRelUtilityGain)
            maxRelUtilityLosses.append(maxRelUtilityLoss)
            maxRelUtilityGainPercentageOfMaxValuation15.append(maxAtValuation)


    countPercentageOfMaxRelUtility = []
    countPercentageOfMaxRelUtility15 = []
    relValueSpace = []
    relValueSpace15 = []
    for i in range(1001):
        countPercentageOfMaxRelUtility.append(maxRelUtilityGainPercentageOfMaxValuation.count(i/1000)/565)
        countPercentageOfMaxRelUtility15.append(maxRelUtilityGainPercentageOfMaxValuation15.count(i/1000)/833)
        relValueSpace.append(i/1000)
        relValueSpace15.append(i/1000)

    nullIndexes = []
    nullIndexes15 = []
    for i in range(1001):
        if countPercentageOfMaxRelUtility[i] == 0:
            nullIndexes.append(i)
        if countPercentageOfMaxRelUtility15[i] == 0:
            nullIndexes15.append(i)

    list.sort(nullIndexes15, reverse=True)
    list.sort(nullIndexes, reverse=True)
    for j in nullIndexes15:
        del countPercentageOfMaxRelUtility15[j]
        del relValueSpace15[j]
    for j in nullIndexes:
        del countPercentageOfMaxRelUtility[j]
        del relValueSpace[j]

    plt.subplot(1, 2, 1)
    plt.scatter(relValueSpace, countPercentageOfMaxRelUtility,color = 'g',s=3, linewidths=0.2)
    plt.xlabel('Value\n(Percentaged point where the maximal\n relative utility gain is in the value distribution)')
    plt.ylabel('Fraction of deviating Bidders')
    plt.title('8 Items')


    plt.subplot(1, 2, 2)
    plt.scatter(relValueSpace15, countPercentageOfMaxRelUtility15,color = 'b',s=3, linewidths=0.2)
    plt.xlabel('Value\n(Percentaged point where the maximal\n relative utility gain is in the value distribution)')
    plt.ylabel('Fraction of deviating Bidders')
    plt.title('15 Items')
    plt.suptitle('Distribution Of Relative Position Of Maximal Relative Utility Gain')

    plt.show()
    return maxRelUtilityGainPercentageOfMaxValuation, maxRelUtilityGainPercentageOfMaxValuation15

folder = "8itemsUsedInThesis/"
folder15 = "15itemsUsedInThesis/"
finalFile = "028"

# Run this to get the average maximal relative UtilityGain
maxRelUtilityGains15 = calcMaxRelUtilityGains(folder15)
maxRelUtilityGains = calcMaxRelUtilityGains(folder)
# sum = 0
# for i in maxRelUtilityGains:
#     sum += i
# print(sum/len(maxRelUtilityGains)) # this calculates the average utility gain.
boxPlotRelUtilities(maxRelUtilityGains, maxRelUtilityGains15)


#plotOverbidStrategy(folder, file)
#plotFinalOverbidStrategy(folder, finalFile)

#plotSingleStrategy(folder, file)
#plotStrategy(folder, file)

#maxRelAtValuation = plotNumberAtWhichPercentageTheMaximalRelUtilityWas(folder, 400)

# Use these methods to calculate how in how many value points over all 833 deviating bidders there is a deviating bid below the true bid
#howManyTimesOverBidBelowBid(folder, 400)
#howManyTimesOverBidBelowBid(folder15, 400)

#calcHowManyTimesTheCases(folder15, 400)

#plotNumberAtWhichPercentageTheMaximalRelUtilityWas(folder, folder15, 400)




