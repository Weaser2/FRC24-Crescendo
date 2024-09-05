package frc.robot.subsystems.shooter;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.Commands;
import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.SparkPIDController;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import static frc.robot.Constants.BlowerConstants;

public class Blower extends SubsystemBase {
    private final CANSparkMax m_blowerMotor;
    private SparkPIDController m_blowerPIDController;
    private RelativeEncoder m_blowerEncoder;
    private BlowerState m_blowerState;

    public enum BlowerState {
        IDLE, BLOWING, SUCKING
    }

    @SuppressWarnings("removal")
    public Blower() {
        m_blowerMotor = new CANSparkMax(BlowerConstants.kBlowerMotorCANID, MotorType.kBrushless);
        configureBlower();
    }

    private void configureBlower() {
        m_blowerMotor.restoreFactoryDefaults();
        m_blowerMotor.setSmartCurrentLimit(BlowerConstants.kBlowerMotorCurrentLimit);
        m_blowerMotor.setIdleMode(BlowerConstants.kBlowerIdleMode);
        m_blowerMotor.setInverted(BlowerConstants.kBlowerInverted);

        m_blowerPIDController = m_blowerMotor.getPIDController();
        m_blowerEncoder = m_blowerMotor.getEncoder();

        // m_blowerPIDController.setP(BlowerConstants.kP);
        // m_blowerEncoder.setPositionConversionFactor(BlowerConstants.kEncoderPositionFactor);
        // m_blowerEncoder.setVelocityConversionFactor(BlowerConstants.kEncoderVelocityFactor);

        m_blowerState = BlowerState.IDLE;

        m_blowerMotor.burnFlash();
    }

    @Override
    public void periodic() {
        SmartDashboard.putNumber("Blower Speed", m_blowerMotor.get());
        SmartDashboard.putString("Blower State", m_blowerState.toString());

        switch (m_blowerState) {
            case BLOWING:
                blow();
                break;
            case SUCKING:
                suck();
                break;
            case IDLE:
            default:
                idle();
                break;
        }
    }

    public void blow() {
        m_blowerState = BlowerState.BLOWING;
        m_blowerMotor.set(BlowerConstants.kBlowerSpeed);
    }

    public void suck() {
        m_blowerState = BlowerState.SUCKING;
        m_blowerMotor.set(-BlowerConstants.kBlowerSpeed);
    }

    public void idle() {
        m_blowerState = BlowerState.IDLE;
        m_blowerMotor.set(0);
    }

    public void toggleState(BlowerState state) {
        if (m_blowerState == state) {
            m_blowerState = BlowerState.IDLE;
        } else {
            m_blowerState = state;
        }
    }

    public void setUpButtonBinding(CommandXboxController controller) {
        controller.x().onTrue(Commands.runOnce(() -> toggleState(BlowerState.BLOWING), this));
        controller.y().onTrue(Commands.runOnce(() -> toggleState(BlowerState.SUCKING), this));
    }
}