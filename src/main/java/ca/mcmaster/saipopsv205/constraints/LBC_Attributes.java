/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv205.constraints;
  
import static ca.mcmaster.saipopsv205.Constants.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 * 
 * 
 * 
 */
public class LBC_Attributes    {
   
    public String name ;
    
    public HashSet<String>  fractional_PrimaryVariables      = new HashSet<String> (); 
    public HashSet<String>  fractional_SecondaryVariables      = new HashSet<String> ();     
     
    public double maxLHS = ZERO;
    public int dimension = BILLION;
    
    public int numPrimaryVars = ZERO;
    
    public TreeSet<String> allVars = new TreeSet<String> ();
    
    public String toString (){
        String result = "Name " + name   ;
              
        result +="\n     Fractional Primary: ";
        for (String str :  fractional_PrimaryVariables){
            result += str + ", ";    
        }
        
        result +="\n     Fractional Sceondary: ";
        for (String str :  fractional_SecondaryVariables){
            result += str + ", ";    
        }
        
        
        return result+"\n";
    }  
    
     
    
}
