package element;

public class Sensor extends Node implements Comparable {

	private double Emax;
	private double Emin;
	private double E0;
	private double pi;
	private double EArrive;
	private double EExtra;
	private double EAfterCharge;
	private double EAfterRound;

	public Sensor(int id, double x, double y, double e0, double pi, double emax, double emin) {
		super(id, x, y);
		this.setE0(e0);
		this.setPi(pi);
		this.setEmax(emax);
		this.setEmin(emin);
	}

	public double getEmax() {
		return Emax;
	}

	public void setEmax(double emax) {
		Emax = emax;
	}

	public double getEmin() {
		return Emin;
	}

	public void setEmin(double emin) {
		Emin = emin;
	}

	public double getE0() {
		return E0;
	}

	public void setE0(double e0) {
		E0 = e0;
	}

	public double getPi() {
		return pi;
	}

	public void setPi(double pi) {
		this.pi = pi;
	}

	public double getEArrive() {
		return EArrive;
	}

	public void setEArrive(double eArrive) {
		EArrive = eArrive;
	}

	public double getEAfterCharge() {
		return EAfterCharge;
	}

	public void setEAfterCharge(double eAfterCharge) {
		EAfterCharge = eAfterCharge;
	}

	public double getEAfterRound() {
		return EAfterRound;
	}

	public void setEAfterRound(double eAfterRound) {
		EAfterRound = eAfterRound;
	}

	public Sensor copy() {
		Sensor s = new Sensor(this.id, this.x, this.y, this.E0, this.pi, this.Emax, this.Emin);
		return s;
	}

	public double getEExtra() {
		return EExtra;
	}

	public void setEExtra(double eExtra) {
		EExtra = eExtra;
	}

	@Override
	public int compareTo(Object o) {
		// TODO Auto-generated method stub
		Sensor other = (Sensor) o;
		Double w1 = this.getE0() / Math.max(this.getPi(), 1e-6);
		double w2 = other.getE0() / Math.max(other.getPi(), 1e-6);
		return w1.compareTo(w2);
	}
}
