import lombok.CompositeLeaf;
import lombok.VisitableNode;
@CompositeLeaf
@VisitableNode
public class File extends Element{


	public File(Folder p, String name) {
		super(p,name);
	}

}
