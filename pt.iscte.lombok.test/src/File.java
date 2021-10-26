import lombok.Composite;
import lombok.Visitor;

@Composite.Leaf
@Visitor.Node
public class File extends Element{


	public File(Folder p, String name) {
		super(p,name);
	}

}
