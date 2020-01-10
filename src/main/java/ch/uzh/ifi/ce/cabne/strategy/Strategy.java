package ch.uzh.ifi.ce.cabne.strategy;

public interface Strategy<Value, Bid> {
	
	Bid getBid(Value v);
	
	Bid getMaxValue();  // changed this to Bid, since I use Single Value and Multiple Bids...
}
