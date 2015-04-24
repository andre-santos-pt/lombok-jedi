import lombok.CompositeComponent;
import lombok.Observable;
import lombok.ObserverNotify;
import lombok.VisitableNode;
@CompositeComponent
@VisitableNode
public abstract class Element implements IElement{
	private String name;
	private Permissions permissions;

	private Element(String name){
		this.name=name;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public Permissions getPermissions() {
		// TODO Auto-generated method stub
		return permissions;
	}
	
	@Observable(type=TestInterface.class,operation="check")
	@Override
	public void rename(String newname) {
		@ObserverNotify()
		int x;
		name = newname;

	}
	@Observable
	@Override
	public void setPermissions(@ObserverNotify Permissions newPermissions) {
		// TODO Auto-generated method stub
		permissions=newPermissions;
	}

}
