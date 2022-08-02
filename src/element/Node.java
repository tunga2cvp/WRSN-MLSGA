package element;

public class Node {
	
	int id;
	double x;
	double y;
	
	public Node(int id, double x, double y) {
		this.setId(id);
		this.setX(x);
		this.setY(y);
	}
	
	public double getDistance(Node node) {
		double a = this.x - node.getX();
		double b = this.y - node.getY();
		return Math.sqrt(a*a + b*b);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
}
