#!/bin/sh
# dummynet configurations

ipfw flush
ipfw add pipe 100 in
ipfw add pipe 200 out

ipfw pipe 100 config delay 5ms plr 0.005 bw 10MBits/s
ipfw pipe 200 config delay 5ms plr 0.005 bw 10MBits/s

