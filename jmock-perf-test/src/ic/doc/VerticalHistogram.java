package ic.doc;

import java.util.List;

public class VerticalHistogram{
    
    public static void plot(List<Double> numbers) {
        int ranges[] = bucket(numbers);
        printVerticalHistogram(largestValueInArray(ranges), ranges);
    }
    
    public static int[] bucket(List<Double> numbers){
        int ranges[] = new int[10];
        for(double num : numbers){
            if(num < 10) ranges[0]++;
            else if(num < 20) ranges[1]++;
            else if(num < 30) ranges[2]++;
            else if(num < 40) ranges[3]++;
            else if(num < 50) ranges[4]++;
            else if(num < 60) ranges[5]++;
            else if(num < 70) ranges[6]++;
            else if(num < 80) ranges[7]++;
            else if(num < 90) ranges[8]++;
            else ranges[9]++;
        }
        return ranges;
    }
    public static int largestValueInArray(int ranges[]){
        int largest = ranges[0];
        for(int i = 1; i < ranges.length; i++)
            if(ranges[i] > largest) largest = ranges[i];
        return largest;
    }
    public static void printVerticalHistogram(int rows, int asterisks[]){

        System.out.printf(" %-7d%-7d%-7d%-7d%-7d%-7d%-7d%-7d%-7d%d",asterisks[0],asterisks[1],
            asterisks[2],asterisks[3],asterisks[4],asterisks[5],asterisks[6],asterisks[7],asterisks[8],asterisks[9]);
        System.out.println();
        while(rows > 0){
            for(int i = 0; i < asterisks.length; i++){
                if(i == 0){
                    if(asterisks[i] < rows) System.out.printf(" %-7s"," ");
                    else System.out.printf(" %-7s","*");
                }
                else{
                    if(asterisks[i] < rows) System.out.printf("%-7s"," ");
                    else System.out.printf("%-7s","*");
                }
            }
            System.out.println();
            rows--;
        }
        System.out.printf("%-6s%-7s%-7s%-7s%-7s%-7s%-7s%-7s%-7s%s","1-10","11-20","21-30",
                                "31-40","41-50","51-60","61-70","71-80","81-90","91-100");
    }
}
