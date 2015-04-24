import java.util.ArrayList;
import java.util.List;

import lombok.Composite;
import lombok.CompositeChildren;
import lombok.VisitableChildren;
import lombok.VisitableNode;

@Composite
@VisitableNode
public class Folder extends Element{

	@CompositeChildren
	@VisitableChildren
	private List<Element> children;
	
	public Folder(Folder parent, String name) {
		super(parent, name);
		children = new ArrayList<Element>();
	}
}
