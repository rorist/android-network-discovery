#!/usr/bin/env python
import sqlite3
import os

try:
  os.remove('oui.db')
except OSError, err:
  print err
conn = sqlite3.connect('oui.db')
c = conn.cursor()
c.execute("create table oui (mac text, vendor text)")

i=0
for line in open("oui.txt"):
  if "base 16" in line:
    i+=1
    mac = line[:6].lower()
    vendor = line[22:].replace("'", "`").strip()
    try:
      c.execute("insert into oui values ('%s', '%s')"%(mac, vendor))
    except sqlite3.OperationalError, err:
      print err 

print "%s records"%i

conn.commit()
c.close()
