package edu.ucla.cs.bigfuzz.customarray.inapplicable.DFOperator;

import scala.Tuple3;

public class map3 {
   public static void main(String[] args) { 
       apply("");
   }
  static final Tuple3 apply(String s) {
       String cols[] = s.split(",");
       int a = Integer.parseInt(cols[1]);

       Tuple3 test = new Tuple3(cols[0], Integer.parseInt(cols[1]), Integer.parseInt(cols[2]));
      return test;
  }
}