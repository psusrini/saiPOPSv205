/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.saipopsv205.heuristics;
 
import static ca.mcmaster.saipopsv205.Constants.*;
import static ca.mcmaster.saipopsv205.Parameters.*;
import ca.mcmaster.saipopsv205.constraints.*; 
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * This class has become too big. Algorithms for bound pushing should be moved into new classes.
 */
public class SaiPOPS {
    
    //fixed variables and their values
    public TreeMap<String, Boolean> fixedVariables ;
    //free variables and their fractional values
    public TreeMap<String, Double>  freeVariables  ;
    public TreeSet<String> fractionalVariables ;
    public TreeMap<Integer, HashSet<LowerBoundConstraint>> mapOfAllConstraintsInTheModel;
    public TreeMap<String, Double>  objectiveFunctionMap;
    
    private boolean  zzqn = true; 
       
    protected  static Logger logger;
     
    static   {
        logger=Logger.getLogger(SaiPOPS.class);
        logger.setLevel(LOGGING_LEVEL);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa =new  
                RollingFileAppender(layout,LOG_FOLDER+SaiPOPS.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(SIXTY);
            logger.addAppender(rfa);
            logger.setAdditivity(false);    
            
        } catch (Exception ex) {
            ///            
            System.err.println("Exit: unable to initialize logging"+ex);       
            exit(ONE);
        }
    }
    
    public SaiPOPS (TreeMap<String, Boolean> fixings ,TreeMap<String, Double>  freeVariables,
            TreeSet<String> fractionalVariables ,
            TreeMap<Integer, HashSet<LowerBoundConstraint>> mapOfAllConstraintsInTheModel ,
            TreeMap<String, Double>  objectiveFunctionMap
            
            
            ){
        this.fixedVariables=fixings;
        this. freeVariables=freeVariables;
        this .fractionalVariables = fractionalVariables;
        this.mapOfAllConstraintsInTheModel=mapOfAllConstraintsInTheModel;
        this . objectiveFunctionMap = objectiveFunctionMap;
        
    }
   
    public  String  getBranchingVariable (   ){
              
        TreeMap<Integer, TreeMap<String, Integer>>  primaryVariableFrequenciesByDimension= 
                new   TreeMap<Integer, TreeMap<String, Integer>>();
        
        int LOWEST_KNOWN_SECONDARY_DIM = BILLION;
        TreeMap<String, Integer>  secondaryVariableFrequencyMap= new   TreeMap<String, Integer>();
        
       
        
        //walk thru all the constraints to collect information
        for (HashSet<LowerBoundConstraint> lbcSet : mapOfAllConstraintsInTheModel.values()){
            for (LowerBoundConstraint lbc : lbcSet){
                LowerBoundConstraint lbcCopy = lbc.getCopy(fixedVariables);
               
                if (null != lbcCopy){
                    
                    lbcCopy.recordFractionalStatus(fractionalVariables);   
                    
                    //System.err.println(lbcCopy);
                     
                    LBC_Attributes attr = lbcCopy.getAttributes(    );
                    
                    //System.err.println(attr);
                    
                    
                    final int DIMENSION = attr.dimension;   
                    if (BILLION == DIMENSION) continue;
                   
                     
                    
                    if (!attr.fractional_SecondaryVariables.isEmpty()){
                        if (DIMENSION <LOWEST_KNOWN_SECONDARY_DIM ){
                            LOWEST_KNOWN_SECONDARY_DIM= DIMENSION;
                            secondaryVariableFrequencyMap.clear();
                        }
                        if (DIMENSION == LOWEST_KNOWN_SECONDARY_DIM ){
                            for (String var: attr.fractional_SecondaryVariables){
                                Integer current = secondaryVariableFrequencyMap.get(var);
                                if (null==current) current = ZERO;
                                secondaryVariableFrequencyMap.put(var,  ONE+current);
                            }                        
                        }
                    }
                    
                    if (!attr.fractional_PrimaryVariables.isEmpty()){
                        TreeMap<String, Integer> primaryFrequencyMap = primaryVariableFrequenciesByDimension.get( DIMENSION);
                        if (primaryFrequencyMap==null) primaryFrequencyMap = new TreeMap<String, Integer>(); 
                        
                        for (String var: attr.fractional_PrimaryVariables){
                            Integer current = primaryFrequencyMap.get(var);
                            if (null==current) current = ZERO;
                            primaryFrequencyMap.put(var, ONE+current);
                        }  
                        
                        primaryVariableFrequenciesByDimension.put (DIMENSION,primaryFrequencyMap);
                    }
                    
                }
            }
        }
        
        
        //if there are no fractional secondary vars, then find the lowest
        //dimension primary vars having the highest frequency
        //
        //if fractional secondary vars are present, find the
        //highest frequency secondary vars. Then find the 
        //lowest dimension D primary variables which have at least 1 of the secondary vars
        // If D does not exist, pretend like there are no secondary vars, else
        // return the highest freq vars at dimension D in the primary map that are both primary and secondary
        
        TreeSet <String> secondaryVars = new TreeSet <String>    ();
        boolean isMatchFound = false;
        int lowestPrimaryDimension= primaryVariableFrequenciesByDimension.firstKey();
        if (BILLION !=  LOWEST_KNOWN_SECONDARY_DIM){
            secondaryVars .addAll (getHighestFrequencyVars(secondaryVariableFrequencyMap));
            for (Map.Entry<Integer, TreeMap<String, Integer>> entry:
                    primaryVariableFrequenciesByDimension.entrySet()){
                Set<String> temporaryKeySet = new TreeSet<String> ();
                temporaryKeySet.addAll( entry.getValue().keySet());
                temporaryKeySet.retainAll(secondaryVars);
                if (!temporaryKeySet.isEmpty()){
                    lowestPrimaryDimension= entry.getKey();
                    isMatchFound = true;
                    break;
                }
            }
        }
       
        TreeSet <String> candidates ;
        if ( ! isMatchFound){
            candidates =  
                    getHighestFrequencyVars( 
                            primaryVariableFrequenciesByDimension.get(lowestPrimaryDimension));
        }else {
            candidates =  
                    getHighestFrequencyVars( 
                            primaryVariableFrequenciesByDimension.get(lowestPrimaryDimension), 
                            secondaryVars);
        }
        
        //tiebreak on obj
        candidates=getHighestObjMagn (     candidates  );
       
        //random tiebreak        
        String[] candidateArray = candidates.toArray(new String[ZERO]);        
        return candidateArray[ PERF_VARIABILITY_RANDOM_GENERATOR.nextInt(candidates.size())];
    }
      
    private TreeSet<String>  getHighestFrequencyVars  (  TreeMap < String, Integer> frequencyMap  ){
        TreeSet<String>  highestFreqVars= new  TreeSet<String>  ();
        int HIGHEST_KNOWN_FREQUENCY = -ONE;
          
        for (Map.Entry < String, Integer> entry : frequencyMap.entrySet() ){
            String thisVar = entry.getKey();
            int thisFreq = entry.getValue();
              
            if (thisFreq > HIGHEST_KNOWN_FREQUENCY){
                HIGHEST_KNOWN_FREQUENCY= thisFreq;
                highestFreqVars.clear();
            }
            if (thisFreq == HIGHEST_KNOWN_FREQUENCY){
               highestFreqVars .add (thisVar );
            }
        }
          
        return highestFreqVars; 
    }
        
    private TreeSet<String>    getHighestFrequencyVars  (  TreeMap < String, Integer> frequencyMap ,TreeSet<String> candidates ){
       
        double LARGEST_KNOWN_FREQ = - ONE;
        TreeSet<String>  winners = new TreeSet<String>();
        
        for (String thisVar : candidates){
            Integer thisFreq =  frequencyMap.get( thisVar) ;
            if (null==thisFreq) thisFreq  =ZERO;
            
            if (thisFreq> LARGEST_KNOWN_FREQ){
                LARGEST_KNOWN_FREQ =thisFreq;
                winners.clear();
            }
            if (thisFreq == LARGEST_KNOWN_FREQ){
                winners.add (thisVar);
            }
        }
           
        return   (winners ) ;
    }   
    
    private TreeSet<String>    getHighestObjMagn ( Set<String>    candidates  ){
       
        double LARGEST_KNOWN_OBJ_MAGN = - ONE;
        TreeSet<String>  winners = new TreeSet<String>();
       
        for (String thisVar : candidates){
            double thisObjMagn = Math.abs (this.objectiveFunctionMap.get(thisVar));
            if (thisObjMagn> LARGEST_KNOWN_OBJ_MAGN){
                LARGEST_KNOWN_OBJ_MAGN =thisObjMagn;
                winners.clear();
            }
            if (thisObjMagn == LARGEST_KNOWN_OBJ_MAGN){
                winners.add (thisVar);
            }
        }
           
        return   (winners ) ;
    }
   

}

