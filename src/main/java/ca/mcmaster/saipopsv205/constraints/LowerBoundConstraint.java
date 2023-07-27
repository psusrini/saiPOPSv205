package ca.mcmaster.saipopsv205.constraints;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

  
import static ca.mcmaster.saipopsv205.Constants.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
 
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author tamvadss
 */
public class LowerBoundConstraint {
    public String constraint_Name ;
    public List <Triplet> coefficientList  =   new   ArrayList <Triplet>  ();
    
    private TreeMap <String, Double > variableMap = new TreeMap <String, Double >();   
    private TreeMap < String , Integer> indexInto_coefficientList  = 
            new         TreeMap < String , Integer>();    
           
    private double lowerBound ;
     
    public LowerBoundConstraint (double lowerBound , String name) {
        this.lowerBound = lowerBound;
        constraint_Name = name;
    }
    
    public String toString (){
        
        String result= "\n-----------------------\n LBC " + constraint_Name + ": ";
        for (Triplet triplet : this.coefficientList) {
            result += triplet.constraintCoefficient + " "+ triplet.varName + (triplet.isFractional ?  "(f)": "" ) +   " + ";
        }
        result+= " -"+ this.lowerBound;
        return result ;
    }
    
    
    public void add (Triplet triplet ) {       
               
        boolean cond1= (triplet.objectiveCoeffcient >= ZERO && triplet.constraintCoefficient > ZERO);
        boolean cond2= (triplet.objectiveCoeffcient < ZERO && triplet.constraintCoefficient < ZERO);
        
        if (   cond1 || cond2     ) {
            triplet.isPrimary = true;     
            
        }  
        
        this.variableMap.put (triplet.varName,  triplet.constraintCoefficient) ;
        this.indexInto_coefficientList .put (triplet.varName, coefficientList.size() );
        this.coefficientList .add (triplet) ;  
         
    }
   
    
    //argument is variables that are already fixed to either 0 or 1
    public void applyKnownFixing  ( String variable, Boolean value ) {
        Double coeff = this.variableMap.remove( variable);
        
        if (null!=coeff){
            if (  value) this.lowerBound -= coeff;
            
            int position = indexInto_coefficientList.remove(variable);
            Triplet triplet = coefficientList.remove (position );
            for (Map.Entry <String, Integer> entry : indexInto_coefficientList.entrySet()){
                if (entry.getValue() > position){
                    entry.setValue( entry.getValue()-ONE);
                }
                
            }    
            
            
        }
        
    }
    
    public void applyKnownFixings  (TreeMap<String, Boolean> fixings ) {
        for ( Map.Entry<String, Boolean> entry : fixings.entrySet()){
            applyKnownFixing (entry.getKey(), entry.getValue() );
        }
    }
    
    public void recordFractionalStatus (TreeSet<String> fractionalVariables){
        for (Triplet triplet: this.coefficientList ){
            if (fractionalVariables.contains(triplet.varName)){
                triplet.isFractional = true;
            } else triplet.isFractional = false;
        }
        
    }
     
    // copy this constraint into another
    //
    // used by every node in the cplex search tree to get its own copy of every constraint
    //    
    public LowerBoundConstraint getCopy ( TreeMap<String, Boolean> fixings) {
        LowerBoundConstraint twin = new LowerBoundConstraint ( this.lowerBound,this.constraint_Name);
        
        twin.variableMap.putAll(this.variableMap);
        twin.coefficientList .addAll(this.coefficientList);
        twin.indexInto_coefficientList .putAll(indexInto_coefficientList );
        
       
        twin.applyKnownFixings(fixings);             
        
        return twin.coefficientList.size () < TWO ?  null: twin;
    }
    
        
    public int getVariableCount () {
        return this.coefficientList.size();
                
    }
    
  
    
    
    public double getLB(){
        return this.lowerBound;
    }
             
    public void sort () {
        
        //for pessimistic dimensioning
        
        Collections.sort(this.coefficientList);
                          
        //rebuild the index
        this.indexInto_coefficientList.clear();
        int index = ZERO;
        for (Triplet triplet : this.coefficientList){
            indexInto_coefficientList.put(triplet.varName , index );
            index ++;
        }
        
    }
        
    
  
    
    public LBC_Attributes getAttributes (    ){
        
        LBC_Attributes attr = new LBC_Attributes ();         
        attr.name = this.constraint_Name;
        
        for (Triplet triplet : this.coefficientList ){
            
            if (triplet.isPrimary) attr.numPrimaryVars ++;
            
            attr.allVars.add(triplet.varName);
            
            if (ZERO < triplet.constraintCoefficient ) attr.maxLHS += triplet.constraintCoefficient;
               
            if ( triplet.isFractional ){                   
                if (triplet.isPrimary   ){    
                    attr.fractional_PrimaryVariables.add (triplet.varName);                     
                }  else {
                    attr.fractional_SecondaryVariables.add (triplet.varName);                   
                }
            } 
             
        }         
      
         //x
         
        attr.dimension =  getDimension(  attr.maxLHS    ) ;
        return attr;    
    
    }
    
       
    private int getDimension (double maxLHS   ){
       
        int dimension = ZERO;
        double highestPossibleLHS=maxLHS ;
        
        for (Triplet triplet : this.coefficientList ){       
            
         
            
            dimension ++;
            
            
            
            highestPossibleLHS -= Math.abs (triplet.constraintCoefficient );
            if (highestPossibleLHS < this.lowerBound) break;
        }
        
         
        
        return (highestPossibleLHS < this.lowerBound)  ? dimension : BILLION ;
    }
      
   
  
}
