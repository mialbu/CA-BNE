#!/usr/bin/python3

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


def plot_bne(nr):
    folder = "5-4-4/"
    stratsfile = folder + nr + ".strats"
    logfile = folder + nr + ".log"
    bundles = []

    with open(logfile) as log:
        runtime = log.readline().rstrip().split(" ")[1]
        converged = log.readline().rstrip().split(" ")[1]
        epsilon = log.readline().rstrip().split(" ")[1]
        nr_players = int(log.readline().split(" ")[1])
        nr_items = int(log.readline().split(" ")[1])
        log.readline()
        for player in log.readlines():
            bundles.append("[" + player.rstrip().split(" ")[1] + "]")

    max_player = 0
    data = []
    nr_iters = 0
    with open(stratsfile) as fd:
        fd.readline()
        for line in fd.readlines():
            if line.rstrip().split("  ")[0] == '':
                data.append("Infinity")
                continue
            if len(line.rstrip().split(" ")) == 1:
                data.append(float(line.rstrip().split(" ")[0]))
                continue
            base, *xy = line.split("  ")
            current_player = int(base.split(" ")[1])
            max_player = max(current_player, max_player)
            xy = xy[:-1]  # cut '\n' at end

            itr = iter(xy)
            xy_pairs = []
            for it in itr:
                pair = (float(it.split(' ')[0]), float(it.split(' ')[1]))
                xy_pairs.append(pair)
                xx, yy = zip(*xy_pairs)

            if current_player == 0:
                nr_iters += 1
            data.append((xx, yy))
    fig = plt.figure(figsize=(10, 8))

    def anim_update(j):
        fig.clear()
        plt.xlim(0.0, nr_items)
        plt.ylim(0.0, nr_items)

        plt.xticks(np.arange(0, nr_items + 0.01, 0.5))
        plt.yticks(np.arange(0, nr_items + 0.01, 0.5))
        for pl in range(max_player + 2):
            if pl == 0:
                current_eps = float(data[(max_player + 2) * j + pl])
                continue
            xxx, yyy = data[(max_player + 2) * j + pl]
            if len(xxx) == 1:
                xxx = (xxx[0], xxx[0])
                yyy = (yyy[0], yyy[0])
                cur_label = "Bidder " + str(pl) + ": no interest"
                plt.plot(xxx, yyy, "-", clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label=cur_label)
            else:
                cur_label = "Bidder " + str(pl) + ": " + bundles[pl-1]
                plt.plot(xxx, yyy, "-", clip_box=mpl.transforms.Bbox([[0, 0], [0.1, 0.3]]), clip_on=True, label=cur_label)
        plt.title("Strategies (" + nr + ".strats)\nconverged: " + converged + "\nIteration: " + str(j) + "\n" + "%.5f" % current_eps)
        plt.xlabel("Value for Bundle")
        plt.ylabel("Best bid on Bundle")
        plt.legend()

    a = anim.FuncAnimation(fig, anim_update, frames=nr_iters, repeat=False, interval=600)
    plt.show()


for i in range(2):
   plot_bne("0" + str(i+3))
# plot_bne("09")
