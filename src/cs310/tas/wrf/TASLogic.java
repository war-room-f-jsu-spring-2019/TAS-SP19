package cs310.tas.wrf;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import org.json.simple.*;

/**
 * The TASLogic class contains functions that will be needed for use by  
 * payroll, mangers, etc.
 * @author War Room F
 */
public class TASLogic {
    

    /**
     * Constant variable for the punch type of a clock-in.
     */
    public static final int CLOCKIN = 1;

    /**
     * Constant variable for the punch type of a clock-out.
     */
    public static final int CLOCKOUT = 0;
    
    /**
     * Calculates the total minutes accrued by an employee on a single shift.
     * @param dailypunchlist an ArrayList of Punch objects for a shift
     * @param shift a shift object containing the shift rules
     * @return the total amount of minutes accrued in one shift as an int
     */

    public static int calculateTotalMinutes(ArrayList<Punch> dailypunchlist, 
            Shift shift) {

        int totalMin = 0;
        long totalMillis = 0;
        long inTime = 0;
        long outTime = 0;
        int punchCounter = 0;
        int lunchTime = shift.totalLunchTime();
    
        for(int i = 0; i < dailypunchlist.size(); i++) {
            
           if (dailypunchlist.get(i).getPunchtypeid() == CLOCKIN) {           
               inTime = dailypunchlist.get(i).getAdjustedTimeStamp().getTime();
               punchCounter++;
               continue;        
           }

           if (dailypunchlist.get(i).getPunchtypeid() == CLOCKOUT) {
               outTime = dailypunchlist.get(i).getAdjustedTimeStamp().getTime();
               punchCounter++;          
           }
           
           if (inTime != 0 && outTime != 0) {         
               totalMillis += outTime - inTime;              
           }                
           inTime = 0;
           outTime = 0;
           
        }
        
        if (totalMillis != 0) {           
            totalMin = (int) (totalMillis/60000);         
        }
        
        if (totalMin > shift.getlunchDeduct() && punchCounter <= 3) {       
            totalMin -= lunchTime;           
        }
        return totalMin;
        
    }    
    
    /**
     * 
     * @param dailyPunchList an ArrayList of Punch objects for a shift
     * @return the daily punch list as a a String in JSON format
     */
    public static String getPunchListAsJSON(ArrayList<Punch> dailyPunchList){

        ArrayList<HashMap<String, String>> jsonData = new ArrayList<>();
        for(Punch p : dailyPunchList){
            HashMap<String, String> punchData = new HashMap<>();
            punchData.put("id", String.valueOf(p.getId()));
            punchData.put("badgeid", p.getBadgeid());
            punchData.put("terminalid", String.valueOf(p.getTerminalid()));
            punchData.put("punchtypeid", String.valueOf(p.getPunchtypeid()));
            punchData.put("punchdata", p.getAdjustMessage());
            punchData.put("originaltimestamp", Long.toString(p.getOriginaltimestamp()));
            punchData.put("adjustedtimestamp", Long.toString(p.getAdjustedTimeStamp().getTime()));
            
            jsonData.add(punchData);
            
        }
        return JSONValue.toJSONString(jsonData);
    }
    
    /**
     * 
     * @param punchlist an ArrayList of Punch objects for a shift
     * @param shift a Shift object containing the shift rules
     * @return the calculated absenteeism of a shift as a double by looking at
     * the punch list of a shift
     */
    public static double calculateAbsenteeism(ArrayList<Punch> punchlist, Shift shift) {
        
        double totalMin = 0;
        ArrayList<ArrayList<Punch>> punches = new ArrayList<ArrayList<Punch>>();
        ArrayList<Punch> tempList1 = new ArrayList<Punch>();
        ArrayList<Punch> tempList2 = new ArrayList<Punch>();
        ArrayList<Punch> tempList3 = new ArrayList<Punch>();
        ArrayList<Punch> tempList4 = new ArrayList<Punch>();
        ArrayList<Punch> tempList5 = new ArrayList<Punch>();
        ArrayList<Punch> tempList6 = new ArrayList<Punch>();
        
        for(Punch p: punchlist) {       
            Timestamp t = new Timestamp(p.getOriginaltimestamp());
            LocalDateTime t1 = t.toLocalDateTime();
            String day = t1.getDayOfWeek().toString();
            switch(day) {
                case "MONDAY":
                    tempList1.add(p);
                    break;
                case "TUESDAY":
                    tempList2.add(p);
                    break;
                case "WEDNESDAY":
                    tempList3.add(p);
                    break;
                case "THURSDAY":
                    tempList4.add(p);
                    break;
                case "FRIDAY":
                    tempList5.add(p);
                    break;
                case "SATURDAY":
                    tempList6.add(p);
                    break;
            } 
            
        }
        
        punches.add(tempList1);
        punches.add(tempList2);
        punches.add(tempList3);
        punches.add(tempList4);
        punches.add(tempList5);
        punches.add(tempList6);
        
        for(ArrayList<Punch> a: punches)
            totalMin += calculateTotalMinutes(a, shift);
        
        double absenteeism = 2400 - totalMin;
        double percentage = (absenteeism/2400 )*100;
        return percentage;
        
    }
    
    /**
     * 
     * @param punchlist an ArrayList of Punch objects for a shift
     * @param s a Shift object containing the shift rules
     * @return the daily punch list with an absenteeism percentage as a a String
     * in JSON format
     */
    public static String getPunchListPlusTotalsAsJSON(ArrayList<Punch> punchlist, Shift s) {
        
        ArrayList<HashMap<String, String>> jsonData = new ArrayList<>();
        double absenteeism = calculateAbsenteeism(punchlist, s);
        String a = String.format("%.2f", absenteeism);
        a = a+ "%";
        
        int totalMin = 0;
        totalMin = (int) ((absenteeism/100)*2400);
        totalMin = 2400 - totalMin;
     
        for(Punch p : punchlist){
           
            HashMap<String, String> punchData = new HashMap<>();
            punchData.put("terminalid", String.valueOf(p.getTerminalid()));
            punchData.put("badgeid", p.getBadgeid());
            punchData.put("id", String.valueOf(p.getId()));
            punchData.put("punchtypeid", String.valueOf(p.getPunchtypeid()));
            punchData.put("punchdata", p.getAdjustMessage());
            punchData.put("originaltimestamp", Long.toString(p.getOriginaltimestamp()));
            punchData.put("adjustedtimestamp", Long.toString(p.getAdjustedTimeStamp().getTime()));
            
            jsonData.add(punchData);
            
        }
        HashMap<String, String> data = new HashMap<>();
        data.put("absenteeism", a);
        data.put("totalminutes", String.valueOf(totalMin));
        jsonData.add(data);
        
        return JSONValue.toJSONString(jsonData);
        
    }
    
}
