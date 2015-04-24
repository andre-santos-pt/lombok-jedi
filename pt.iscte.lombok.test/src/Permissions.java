import lombok.Flyweight;
import lombok.FlyweightObject;

@Flyweight
public class Permissions {
@FlyweightObject
int read;
@FlyweightObject
int write;
@FlyweightObject
int execute;
}
