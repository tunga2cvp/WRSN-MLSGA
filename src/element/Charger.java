package element;

public class Charger {
	private double E0;
	private double speed;
	private double Pm; // the per-second energy consumption of the MC when traveling
	private double U; // the per-second energy that the MC charges a sensor
	
	public Charger(double e0, double speed, double pm, double u) {
		this.setE0(e0);
		this.setPm(pm);
		this.setSpeed(speed);
		this.setU(u);
	}
	
	public double getTotalChargingTime(double travelingDistance) {
		return (this.getE0() - travelingDistance*this.getPm()/getSpeed())/this.getU(); 
	}
	
	public double getE0() {
		return E0;
	}
	public void setE0(double e0) {
		E0 = e0;
	}
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public double getPm() {
		return Pm;
	}
	public void setPm(double pm) {
		Pm = pm;
	}
	public double getU() {
		return U;
	}
	public void setU(double u) {
		U = u;
	}
	
	
}
