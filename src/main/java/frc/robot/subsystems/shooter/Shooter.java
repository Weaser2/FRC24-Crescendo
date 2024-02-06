package frc.robot.subsystems.shooter;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.CANSparkFlex;
import com.revrobotics.CANSparkMax;
import com.revrobotics.SparkPIDController;
import com.revrobotics.CANSparkLowLevel.MotorType;

import frc.robot.Constants;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.units.Measure;
import edu.wpi.first.units.Velocity;
import edu.wpi.first.units.Angle;
import edu.wpi.first.units.Distance;
import static edu.wpi.first.units.Units.*;

public class Shooter extends SubsystemBase {
    private CANSparkMax m_feederMotor;
    private CANSparkFlex m_kickerMotor;
    private CANSparkFlex m_shooterLeftMotor;
    private CANSparkFlex m_shooterRightMotor;

    private AbsoluteEncoder pivotAngle;
    private RelativeEncoder leftShotSpeed;
    private RelativeEncoder rightShotSpeed;

    private SparkPIDController m_feederPIDController;
    private SparkPIDController m_kickerPIDController;
    private SparkPIDController m_shooterLeftPIDController;
    private SparkPIDController m_shooterRightPIDController;

    // desiredPivot should always be in degrees from the horizontal plane.
    private Measure<Angle> desiredPivotAngle;
    private Measure<Velocity<Distance>> desiredShotSpeed;
    private Measure<Velocity<Angle>> desiredRotationSpeed;

    public Shooter() {
        m_feederMotor = new CANSparkMax(Constants.ShooterConstants.feederMotorCANID, MotorType.kBrushless);
        m_kickerMotor = new CANSparkFlex(Constants.ShooterConstants.kickerMotorCANID, MotorType.kBrushless);
        m_shooterLeftMotor = new CANSparkFlex(Constants.ShooterConstants.shooterLeftCANID, MotorType.kBrushless);
        m_shooterRightMotor = new CANSparkFlex(Constants.ShooterConstants.shooterRightCANID, MotorType.kBrushless);

        m_feederMotor.restoreFactoryDefaults();
        m_kickerMotor.restoreFactoryDefaults();
        m_shooterLeftMotor.restoreFactoryDefaults();
        m_shooterRightMotor.restoreFactoryDefaults();

        m_shooterLeftPIDController = m_shooterLeftMotor.getPIDController();
        m_shooterRightPIDController = m_shooterRightMotor.getPIDController();
        m_kickerPIDController = m_kickerMotor.getPIDController();
        m_feederPIDController = m_feederMotor.getPIDController();

        rightShotSpeed = m_shooterRightMotor.getEncoder();
        leftShotSpeed = m_shooterLeftMotor.getEncoder();
        leftShotSpeed.setVelocityConversionFactor(1 / (120 * Math.PI));
        rightShotSpeed.setVelocityConversionFactor(1 / (120 * Math.PI));
        pivotAngle.setPositionConversionFactor(360);

        desiredPivotAngle = Degrees.of(0);
        desiredShotSpeed = MetersPerSecond.of(0.0);
        desiredRotationSpeed = RadiansPerSecond.of(0.0);
        // Set PID values
    }

    // TODO: spinFeeder
    private void spinFeeder(Measure<Velocity<Angle>> speed) {
        m_feederPIDController.setReference(speed.in(RPM), CANSparkFlex.ControlType.kVelocity);
    }

    // TODO: spinKicker
    private void spinKicker(Measure<Velocity<Angle>> speed) {
        m_kickerPIDController.setReference(speed.in(RPM), CANSparkFlex.ControlType.kVelocity);
    }

    // TODO: spinShooter
    private void spinShooterAngular(Measure<Velocity<Angle>> leftSpeed, Measure<Velocity<Angle>> rightSpeed) {
        m_shooterLeftPIDController.setReference(leftSpeed.in(RPM), CANSparkFlex.ControlType.kVelocity);
        m_shooterRightPIDController.setReference(rightSpeed.in(RPM), CANSparkFlex.ControlType.kVelocity);
    }

    /**
     * Spins shooter wheels such that the tangential velocity at a point along the
     * circumference is the velocity given in meters per second.
     * 
     * @param leftLinear
     * @param rightLinear
     */
    private void spinShooterLinear(Measure<Velocity<Distance>> leftLinear, Measure<Velocity<Distance>> rightLinear) {
        Measure<Velocity<Angle>> leftAngular = wheelSpeedToRotation(leftLinear,
                Constants.ShooterConstants.shooterWheelRadius);
        Measure<Velocity<Angle>> rightAngular = wheelSpeedToRotation(rightLinear,
                Constants.ShooterConstants.shooterWheelRadius);
        spinShooterAngular(leftAngular, rightAngular);
    }

    /**
     * converts a linear velocity to a rotational speed based on the provided
     * radius.
     * 
     * @param speed
     * @param radius
     * @return
     */
    public Measure<Velocity<Angle>> wheelSpeedToRotation(Measure<Velocity<Distance>> speed, Measure<Distance> radius) {
        return RadiansPerSecond.of(speed.in(MetersPerSecond) / radius.in(Meters));
    }

    /**
     * converts a rotational speed to a linear velocity based on the provided
     * radius.
     * @param speed
     * @param radius
     * @return
     */
    public Measure<Velocity<Distance>> wheelRotationToSpeed(Measure<Velocity<Angle>> speed, Measure<Distance> radius) {
        return MetersPerSecond.of(speed.in(RadiansPerSecond) * radius.in(Meters));
    }

    public void idle() {
        m_shooterLeftMotor.stopMotor();
        m_shooterRightMotor.stopMotor();
        m_kickerMotor.stopMotor();
        spinFeeder(RPM.of(240));
    }

    public void stopAll() {
        m_feederMotor.stopMotor();
        m_kickerMotor.stopMotor();
        m_shooterLeftMotor.stopMotor();
        m_shooterRightMotor.stopMotor();
    }

    /**
     * Spins the shooter wheels based on a given launch speed and rotational speed of the note.
     */
    public void setShootSpeed(Measure<Velocity<Distance>> launchSpeed, Measure<Velocity<Angle>> rotationalSpeed) {
        //num represents the change in the linear speed required to rotate the note at the provided rotationalSpeed.
        double num = wheelRotationToSpeed(rotationalSpeed, Inches.of(12)).in(MetersPerSecond);
        Measure<Velocity<Distance>> leftSpeed = MetersPerSecond.of(launchSpeed.in(MetersPerSecond) - num);
        Measure<Velocity<Distance>> rightSpeed = MetersPerSecond.of(launchSpeed.in(MetersPerSecond) + num);
        spinShooterLinear(leftSpeed, rightSpeed);
    }

    public void anglePivot(double angle) {

    }

    /**
     * Calculates the output speed of the shooter required to shoot in the speaker from the
     * robot's current position.
     * 
     * Should take in the robots position on the feild given by Swerve Odometry.
     */
    public void calcShotSpeed() {
        desiredShotSpeed = MetersPerSecond.of(4.0);
    }

    /**
     * Calculates the pivot of the shooter required to shoot in the speaker from the
     * robot's current position.
     * 
     * Should take in the robots position on the feild given by Swerve Odometry.
     * @return
     */
    public void calcPivot() {
        desiredPivotAngle = Degrees.of(0.0);
    }

    /**
     * While called, angles the pivot of the shooter and sets the shooter to the output speed neccissary
     * to score from the bot's distance from the shooter. However, does not shoot the note. 
     */
    public void speakerMode() {
        calcPivot();
        calcShotSpeed();
        // Add a line here to angle the pivot.
        Measure<Velocity<Distance>> lSpeed = MetersPerSecond.of(desiredShotSpeed.in(MetersPerSecond)
                - wheelRotationToSpeed(desiredRotationSpeed, Inches.of(12)).in(MetersPerSecond));
        Measure<Velocity<Distance>> rSpeed = MetersPerSecond.of(desiredShotSpeed.in(MetersPerSecond)
                + wheelRotationToSpeed(desiredRotationSpeed, Inches.of(12)).in(MetersPerSecond));
        spinShooterLinear(lSpeed, rSpeed);
    }

    /**
     * Returns true if the current position of the pivot of the shooter is within
     * the tolerance of the desired pivot angle.
     * Tolerance in degrees is defined in ShooterConstants.
     * 
     * @return
     */
    private boolean getIsAtDesiredPivotAngle() {
        double pos = pivotAngle.getPosition();
        double tol = Constants.ShooterConstants.pivotToleranceDegrees;
        if (pos <= desiredPivotAngle.in(Degrees) + tol && pos >= desiredPivotAngle.in(Degrees) - tol) {
            return true;
        }
        return false;
    }

    private boolean getIsAtLeftShooterSpeed() {
        double desired = desiredShotSpeed.in(MetersPerSecond)
                - wheelRotationToSpeed(desiredRotationSpeed, Inches.of(12)).in(MetersPerSecond);
        double tol = Constants.ShooterConstants.launchSpeedTolerance.in(MetersPerSecond);
        // Current speed of the left wheel
        double speed = wheelRotationToSpeed(RadiansPerSecond.of(leftShotSpeed.getVelocity()),
                Constants.ShooterConstants.shooterWheelRadius).in(MetersPerSecond);
        if (speed <= desired + tol && speed >= desired - tol) {
            return true;
        }
        return false;
    }

    private boolean getIsAtRightShooterSpeed() {
        double desired = desiredShotSpeed.in(MetersPerSecond)
                + wheelRotationToSpeed(desiredRotationSpeed, Inches.of(12)).in(MetersPerSecond);
        double tol = Constants.ShooterConstants.launchSpeedTolerance.in(MetersPerSecond);
        // Current speed of the right wheel
        double speed = wheelRotationToSpeed(RadiansPerSecond.of(rightShotSpeed.getVelocity()),
                Constants.ShooterConstants.shooterWheelRadius).in(MetersPerSecond);
        if (speed <= desired + tol && speed >= desired - tol) {
            return true;
        }
        return false;
    }

    private boolean getIsAtShooterSpeed() {
        if (getIsAtLeftShooterSpeed() && getIsAtRightShooterSpeed()) {
            return true;
        }
        return false;
    }

    /**
     * Spins the kicker enough to fire only when the shooter is at the neccissary launch speed and pivot angle.
     */
    public void speakerFire() {
        calcPivot();
        calcShotSpeed();
        if (getIsAtShooterSpeed() && getIsAtDesiredPivotAngle()) {
            spinKicker(Constants.ShooterConstants.kickerSpeed);
        }
    }

}