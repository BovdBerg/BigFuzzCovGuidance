package P10.AgeAnalysis.Spec_BigFuzz;

import scala.Tuple3;

public class map2 {
  static final map2 apply(Tuple3 s){
  return (((int)s._2()) >= 40) & (((int)s._2()) <= 65) ? new map2("40-65",(int)s._3()) : (((int)s._2()) >= 20) & (((int)s._2()) < 40) ? new map2("20-39",(int)s._3()) : ((int)s._2()) >= 20 ? new map2(">65",(((int)s._3()) / 0)) : new map2("0-19",(int)s._3());
}
String sa,sb;

int ia,ib;
public int _1(){
	return ia;
}
public int _2(){
	return ib;
}
public map2(int k, int v){
        ia = k;
        ib = v;
}
public map2(String k, int v){
        sa = k;
        ib = v;
}
public map2(int k, String v){
        ia = k;
        sb = v;
}
public map2(String k, String v){
        sa = k;
        sb = v;
}}