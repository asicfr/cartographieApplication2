package com.carto.applicarto.utils;


import javax.swing.JOptionPane;


public class Comparison 
{
  public static void main(String args[])
  {
    // read first number from user as a string
    String firstNumber = JOptionPane.showInputDialog("Enter first integer:");

    // read second number from user as a string
    String secondNumber = JOptionPane.showInputDialog("Enter second integer:");          

    // convert numbers from type String to type int
    int number1 = Integer.parseInt(firstNumber);
    int number2 = Integer.parseInt(secondNumber);

    // initialize result to the empty string
    String result = "";
    if (number1 == number2)
      result = number1 + " == " + number2;

    if (number1 != number2)
      result = number1 + " != " + number2;

    if (number1 < number2)
      result = result + "\n" + number1 + " < " + number2;

    if (number1 > number2)
      result = result + "\n" + number1 + " > " + number2;

    if (number1 <= number2)
      result = result + "\n" + number1 + " <= " + number2;

    if (number1 >= number2)
      result = result + "\n" + number1 + " >= " + number2;

    // Display results
    JOptionPane.showMessageDialog(null, result, "Comparison Results",
        JOptionPane.INFORMATION_MESSAGE);
  } // end of main()

} // end of Comparison class
