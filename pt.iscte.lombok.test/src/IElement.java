import lombok.VisitableType;

@VisitableType
//@Decorator(interfaceClass=IElement.class, abstractClassName="Tes")
public interface IElement{
public String getName();
public Permissions getPermissions();
public void rename(String newname);
public void setPermissions(Permissions newPermissions);
}
