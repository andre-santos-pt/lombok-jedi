import lombok.Decorator;
import lombok.Visitor;

@Visitor//.Type
//@Decorator(interfaceClass=IElement.class, abstractClassName="Tes")
@Decorator(abstractClassName="Tes")
public interface IElement{
public String getName();
public Permissions getPermissions();
public void rename(String newname);
public void setPermissions(Permissions newPermissions);
}
