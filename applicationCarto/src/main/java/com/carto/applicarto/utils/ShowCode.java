package com.carto.applicarto.utils;

import java.util.TreeMap;


public class ShowCode
{
  private TreeMap<String,ShowLines> listings;


  public ShowCode()
  {  listings = new TreeMap<String,ShowLines>();  }


  public void add(String fnm, String fpath)
  // add fnm-ShowLines pair to map
  {
    if (listings.containsKey(fnm)) {
      return;
    }

    listings.put(fnm, new ShowLines(fnm, fpath));
  }  // end of add()

  
  public String show(String fnm, int lineNum)
  // return the specified line from fnm
  {
    ShowLines lines = listings.get(fnm);
    if (lines == null)
      return null; // (fnm + " not listed");

    return lines.show(lineNum);
  }  // end of show()

    
}  // end of ShowCode class
