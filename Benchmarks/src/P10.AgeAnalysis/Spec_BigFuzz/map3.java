package P10.AgeAnalysis.Spec_BigFuzz;

import scala.Tuple3;

public class map3 {
  static final Tuple3 apply(String s){
  String cols[]=s.split(",");
  return new Tuple3(cols[0],(Integer.parseInt(cols[1])),(Integer.parseInt(cols[2])));
}
}