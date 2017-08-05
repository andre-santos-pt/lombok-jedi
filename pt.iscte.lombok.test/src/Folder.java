import lombok.Composite;
import lombok.Visitor;

import java.util.ArrayList;
import java.util.List;

@Composite
@Visitor.Node
public class Folder extends Element{

	@Composite.Children
	@Visitor.Children
	private List<Element> children;
	
	public Folder(Folder parent, String name) {
		super(parent, name);
		children = new ArrayList<Element>();
	}
}
