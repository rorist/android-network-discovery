#!/usr/bin/env python

'''
 Copyright (C) 2009-2013 Aubort Jean-Baptiste (Rorist)
 Licensed under GNU's GPL 2, see README
'''

import sqlite3, os, urllib, re

db = 'oui.db'
url = 'http://standards.ieee.org/regauth/oui/oui.txt'

#Download the file
webFile = urllib.urlopen(url)
localFile = open(url.split('/')[-1], 'w')
localFile.write(webFile.read())
webFile.close()
localFile.close()

#Create the DB
try:
  os.remove(db)
except OSError, err:
  print err
conn = sqlite3.connect(db)
c = conn.cursor()
c.execute("create table oui (mac text, vendor text)")

for line in open("oui.txt"):
  r = re.search('^\s*([0-9A-Fa-f]*)\s*\(base 16\)\s*(.*)$', line)
  if r:
    mac = r.group(1)
    vendor = r.group(2).replace("'", "`").strip()
    try:
      c.execute("insert into oui values ('%s', '%s')"%(mac, vendor))
    except sqlite3.OperationalError, err:
      print err

#print "%s records"%i

conn.commit()
c.close()
