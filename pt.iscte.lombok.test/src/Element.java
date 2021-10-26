import lombok.Composite;
import lombok.Observable;
import lombok.Visitor;

@Composite.Component
@Visitor.Node
public abstract class Element implements IElement{
	private String name;
	private Permissions permissions;
	Folder p;

	Element(String name){
		this.name=name;
	}

	Element(Folder p, String name){
		this.p=p;
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
		@Observable.Notify()
		int x;
		name = newname;

	}
	@Observable
	@Override
	public void setPermissions(@Observable.Notify Permissions newPermissions) {
		// TODO Auto-generated method stub
		permissions=newPermissions;
	}

}
