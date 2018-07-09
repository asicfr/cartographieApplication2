package test;

public class Stack 
{
  private int store[]; 
  private int max_len;
  private int top;
  
  public Stack() 
  { 
    store = new int[15];	// default size
    max_len = store.length-1;
    top = -1;
  }


  public boolean push(int number)   
  {
    if (top == max_len)
      return false;
    top++;
    store[top] = number;
    return true;
  }


  public boolean pop()   
  {
    if (top == -1)
      return false;
    top--;
    return true;
  }

  
  public int topOf()
  {  return store[top]; }
  

  public boolean isEmpty()
  { return (top == -1); }
  
}  // end of Stack.java
