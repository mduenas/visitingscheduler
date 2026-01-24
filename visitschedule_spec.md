# VisitScheduler - Product Specification Document

## Executive Summary

VisitScheduler is a comprehensive visit scheduling platform designed for proxy scheduling—enabling caregivers, family members, or designated coordinators to manage and schedule visits on behalf of individuals who are unable to do so themselves. The primary use case includes hospital patients, elderly individuals in care facilities, or anyone with medical or personal circumstances that limit their ability to manage their own social calendar.

This specification draws from best-in-class features across medical scheduling software (CERTIFY Health, NexHealth, Acuity), general appointment scheduling (Calendly, SimplyBook.me), caregiver coordination apps (Caring Village, Lotsa Helping Hands), and hospital visitor management systems to create a uniquely competitive solution.

---

## Target Users

### Primary Users (Schedulers/Coordinators)
- **Family caregivers** managing visits for hospitalized or homebound loved ones
- **Professional caregivers** coordinating visitor schedules for patients
- **Social workers** managing visitation for multiple clients
- **Hospital staff** handling patient visitor coordination

### Secondary Users (Visitors)
- Family members and friends requesting visit times
- Healthcare providers scheduling consultations
- Support group members coordinating visits

### Beneficiaries (The Visited)
- Hospital patients with specific visiting restrictions
- Elderly individuals in care facilities
- Homebound individuals with health conditions
- Anyone requiring managed visitation due to medical or personal circumstances

---

## Core Features

### 1. Multi-Role Access System

#### 1.1 Role Hierarchy
| Role | Permissions |
|------|-------------|
| **Administrator** | Full system control, all scheduling rights, restriction management, user management |
| **Primary Coordinator** | Create/modify schedules, set restrictions, approve/deny requests, manage visitor lists |
| **Secondary Coordinator** | Limited scheduling rights, view-only for restrictions, can suggest changes |
| **Approved Visitor** | Self-scheduling within approved parameters, view own appointments |
| **Pending Visitor** | Request visits only, no direct scheduling |

#### 1.2 Coordinator Assignment
- Multiple coordinators per beneficiary (e.g., spouse + adult child)
- Hierarchical approval workflows
- Coordinator handoff scheduling (shift-based coordination)
- Emergency contact escalation chain

### 2. Intelligent Scheduling Engine

#### 2.1 Time Slot Management
- **Configurable visiting windows**: Set specific hours/days when visits are allowed
- **Duration options**: 15min, 30min, 45min, 1hr, 2hr, or custom durations
- **Buffer time**: Automatic gaps between visits (5-60 minutes configurable)
- **Overlap prevention**: Prevent double-booking with intelligent conflict detection
- **Recurring visit patterns**: Daily, weekly, bi-weekly, monthly recurring slots

#### 2.2 Smart Scheduling
- **Optimal slot suggestions**: AI-powered recommendations based on energy levels, medical schedules, and historical preferences
- **Fatigue management**: Automatic limits on consecutive visits
- **Peak time management**: Distribute visits evenly to prevent clustering
- **Recovery time rules**: Enforce minimum rest periods after medical procedures

#### 2.3 Capacity Controls
- **Simultaneous visitor limits**: Max visitors at one time (1-10+ configurable)
- **Daily visit caps**: Maximum visits per day
- **Weekly visit caps**: Maximum visits per week
- **Per-visitor frequency limits**: How often the same person can visit

### 3. Restriction & Rule Engine

#### 3.1 Time-Based Restrictions
- **Blackout dates**: Block specific dates entirely
- **Blackout hours**: Block specific hours each day
- **Medical procedure blocks**: Automatic blocking around scheduled treatments
- **Meal time protection**: Block visits during designated meal times
- **Rest period enforcement**: Mandatory quiet hours

#### 3.2 Visitor-Based Restrictions
- **Approved visitor list** (whitelist): Only pre-approved visitors can request/schedule
- **Denied visitor list** (blocklist): Specific individuals prevented from visiting
- **Conditional approval**: Some visitors require coordinator approval per visit
- **Age restrictions**: Minimum age requirements (e.g., no children under 12)
- **Relationship-based access levels**: Different rules for immediate family vs. friends
- **Health screening requirements**: Visitors must confirm health status

#### 3.3 Location/Context Rules
- **Unit-specific rules**: Different restrictions based on facility unit (ICU vs. general)
- **Room-based capacity**: Limits based on physical space constraints
- **Shared room considerations**: Coordination with roommate schedules

#### 3.4 Custom Rule Builder
- Create complex conditional rules with AND/OR logic
- Rule templates for common scenarios
- Rule inheritance and override capabilities
- Temporary rule adjustments with automatic expiration

### 4. Visitor Request & Approval Workflow

#### 4.1 Request Submission
- Visitors can request specific time slots
- Multiple slot preference submission (1st, 2nd, 3rd choice)
- Reason for visit (optional/required based on settings)
- Expected duration indication
- Number of accompanying guests

#### 4.2 Approval Process
- **Auto-approve**: Whitelisted visitors within parameters auto-confirmed
- **Manual review**: Pending queue for coordinator approval
- **Conditional prompts**: Request additional information before approval
- **Batch approval**: Approve multiple requests simultaneously
- **Delegation**: Forward approval to another coordinator

#### 4.3 Request Status Tracking
- Pending → Approved/Denied/Waitlisted
- Modification request tracking
- Cancellation with reason capture
- No-show tracking and consequences

### 5. Communication Hub

#### 5.1 Automated Notifications
- **Confirmation messages**: Immediate booking confirmation
- **Reminder sequences**: 24hr, 2hr, 30min reminders (configurable)
- **Change alerts**: Instant notification of schedule changes
- **Cancellation notices**: Clear communication of cancelled visits
- **Waitlist notifications**: Alert when slot becomes available

#### 5.2 Notification Channels
- SMS text messages
- Email notifications
- In-app push notifications
- WhatsApp integration (optional)
- Voice call reminders (optional)

#### 5.3 Message Templates
- Customizable message content
- Multi-language support
- Personalization tokens (visitor name, date, time, location, instructions)
- Coordinator signature options

#### 5.4 Direct Messaging
- Secure in-app messaging between coordinator and visitors
- Group messaging to all scheduled visitors
- Broadcast updates for policy changes
- Message read receipts

### 6. Calendar & Availability Management

#### 6.1 Visual Calendar Interface
- Day, week, month view options
- Color-coded visit types and statuses
- Drag-and-drop rescheduling
- Quick slot creation
- Multi-calendar overlay view

#### 6.2 External Calendar Sync
- Two-way sync with Google Calendar
- Microsoft Outlook integration
- Apple iCal support
- Import/export ICS files
- Conflict detection with external calendars

#### 6.3 Medical Schedule Integration
- Import treatment schedules
- Doctor appointment blocking
- Therapy session coordination
- Medication schedule awareness

### 7. Care Circle Coordination

#### 7.1 Team Features
- Shared visibility across care team
- Role-based information access
- Coordinator shift scheduling
- Task assignment and tracking
- Activity logs and history

#### 7.2 Family Coordination
- Family group creation
- Shared responsibility distribution
- Visit coverage gaps identification
- Family-wide notification preferences

### 8. Check-In & Visit Management

#### 8.1 Visitor Check-In
- QR code check-in
- Digital visitor badge generation
- Expected visitor list for security
- Real-time arrival notifications
- Late arrival handling

#### 8.2 Visit Tracking
- Actual visit duration logging
- Visit notes and observations
- Photo capture (optional, with consent)
- Mood/energy level tracking post-visit
- Quality ratings

#### 8.3 Check-Out Process
- Visit completion confirmation
- Follow-up action capture
- Next visit scheduling prompt
- Feedback collection

---

## Advanced Features

### 9. Intelligent Insights & Analytics

#### 9.1 Visit Analytics Dashboard
- Total visits (daily/weekly/monthly trends)
- Visit duration statistics
- Peak visiting times heatmap
- No-show rates and patterns
- Visitor frequency distribution

#### 9.2 Beneficiary Wellbeing Metrics
- Energy level correlation with visits
- Optimal visit timing identification
- Social isolation risk indicators
- Recovery impact analysis

#### 9.3 Coordinator Performance
- Response time metrics
- Approval/denial ratios
- Schedule optimization scores
- Visitor satisfaction tracking

### 10. Telehealth & Virtual Visits

#### 10.1 Video Visit Integration
- Built-in HIPAA-compliant video calling
- Integration with Zoom, Google Meet, FaceTime
- Virtual waiting room
- Multi-party video visits
- Recording options (with consent)

#### 10.2 Hybrid Scheduling
- In-person and virtual visit types
- Automatic video link generation
- Technical requirements communication
- Fallback options for connectivity issues

### 11. Accessibility Features

#### 11.1 Interface Accessibility
- WCAG 2.1 AA compliance
- Screen reader optimization
- High contrast mode
- Font size adjustment
- Keyboard navigation
- Voice control support

#### 11.2 Communication Accessibility
- Large print options for confirmations
- Audio message options
- Simplified interface mode
- Caregiver proxy notifications

### 12. Emergency & Special Situations

#### 12.1 Emergency Protocols
- Emergency contact quick-dial
- One-click visit cancellation (all)
- Emergency lockdown mode
- Critical status updates broadcast
- Medical emergency notification

#### 12.2 End-of-Life Features
- Extended visiting hours mode
- Priority visitor designation
- Increased capacity allowances
- Grief support resource links

#### 12.3 Transition Support
- Facility transfer coordination
- Discharge scheduling
- Home care transition
- Follow-up visit planning

---

## Technical Requirements

### 13. Platform Support

#### 13.1 Applications
- **iOS App**: Native iPhone/iPad application
- **Android App**: Native Android application
- **Web Application**: Responsive browser-based access
- **Progressive Web App**: Offline-capable mobile web

#### 13.2 Minimum Requirements
- iOS 15.0 or later
- Android 10.0 or later
- Modern browsers (Chrome, Safari, Firefox, Edge - last 2 versions)
- Internet connection (with offline mode for viewing)

### 14. Security & Compliance

#### 14.1 Healthcare Compliance
- **HIPAA compliant**: Full healthcare data protection
- **SOC 2 Type II certified**: Security controls verification
- **GDPR compliant**: European privacy regulations
- **CCPA compliant**: California privacy regulations

#### 14.2 Data Security
- End-to-end encryption for messages
- AES-256 encryption at rest
- TLS 1.3 for data in transit
- Regular security audits
- Penetration testing

#### 14.3 Authentication & Access
- Multi-factor authentication (MFA)
- Biometric login (Face ID, fingerprint)
- Single Sign-On (SSO) support
- Session management and timeout
- Password complexity requirements
- Brute force protection

#### 14.4 Audit & Logging
- Complete audit trail
- User action logging
- Access attempt tracking
- Export capabilities for compliance

### 15. Integration Ecosystem

#### 15.1 Healthcare System Integration
- HL7 FHIR support
- Epic MyChart integration
- Cerner integration
- athenahealth integration
- Generic EHR/EMR API support

#### 15.2 Calendar Integrations
- Google Calendar
- Microsoft Outlook/Office 365
- Apple Calendar
- CalDAV support

#### 15.3 Communication Integrations
- Twilio (SMS)
- SendGrid (Email)
- WhatsApp Business API
- Slack notifications
- Microsoft Teams

#### 15.4 API & Webhooks
- RESTful API for third-party integration
- GraphQL API (optional)
- Webhook support for real-time events
- API rate limiting and authentication
- Developer documentation and sandbox

---

## User Experience Design

### 16. Design Principles

#### 16.1 Core UX Principles
- **Simplicity first**: Complex features hidden until needed
- **Mobile-first design**: Optimized for smartphone use
- **Accessibility by default**: Inclusive design for all abilities
- **Minimal friction**: Reduce clicks/taps to complete tasks
- **Clear feedback**: Immediate confirmation of actions

#### 16.2 Information Architecture
- Dashboard-centric navigation
- Contextual actions and shortcuts
- Consistent navigation patterns
- Progressive disclosure of complex features

### 17. Key User Flows

#### 17.1 Coordinator Flow
1. Dashboard overview → Today's schedule at a glance
2. Pending requests → Quick approve/deny with swipe
3. Calendar view → Manage upcoming visits
4. Restrictions → Adjust rules as needed
5. Messages → Communicate with visitors
6. Reports → Review analytics

#### 17.2 Visitor Flow
1. Invitation → Accept and create account
2. View availability → See open slots
3. Request visit → Submit preferred times
4. Receive confirmation → Get approved slot details
5. Pre-visit reminder → Receive notification
6. Check-in → Confirm arrival
7. Post-visit → Provide feedback

---

## Competitive Differentiation

### 18. Unique Value Propositions

| Feature | VisitScheduler | Calendly | Caring Village | Hospital VMS |
|---------|----------------|----------|----------------|--------------|
| Proxy scheduling focus | ✅ Primary | ❌ | ⚠️ Limited | ❌ |
| Medical restriction engine | ✅ | ❌ | ❌ | ⚠️ Security only |
| Fatigue management AI | ✅ | ❌ | ❌ | ❌ |
| Multi-coordinator support | ✅ | ⚠️ Team plans | ✅ | ❌ |
| Visitor approval workflows | ✅ | ❌ | ⚠️ Basic | ✅ |
| Care circle coordination | ✅ | ❌ | ✅ | ❌ |
| HIPAA compliance | ✅ | ⚠️ BAA available | ❌ | ✅ |
| Virtual visit integration | ✅ | ⚠️ Zoom only | ❌ | ❌ |
| Wellbeing analytics | ✅ | ❌ | ❌ | ❌ |
| End-of-life features | ✅ | ❌ | ❌ | ❌ |

### 19. Key Differentiators

1. **Purpose-Built for Proxy Scheduling**: Unlike general scheduling tools, designed specifically for someone managing visits on behalf of another person

2. **Medical-Aware Intelligence**: Understands healthcare contexts, treatment schedules, and recovery needs

3. **Empathetic Design**: Features like fatigue management and end-of-life support show deep understanding of user needs

4. **Flexible Restriction Engine**: Most powerful rule system for managing complex visiting requirements

5. **Care Circle First**: Built for collaborative caregiving from the ground up

---

## Monetization Strategy

### 20. Pricing Tiers

#### 20.1 Free Tier - "Family Basic"
- 1 beneficiary
- 2 coordinators
- 20 visits/month
- Basic scheduling
- Email notifications only
- 7-day analytics

#### 20.2 Premium Tier - "Family Plus" ($9.99/month)
- 1 beneficiary
- 5 coordinators
- Unlimited visits
- Full restriction engine
- SMS + Email notifications
- 90-day analytics
- Virtual visit integration
- Priority support

#### 20.3 Professional Tier - "Care Pro" ($29.99/month)
- Up to 5 beneficiaries
- Unlimited coordinators
- All Premium features
- EHR integration
- API access
- Custom branding
- HIPAA BAA included
- 1-year analytics
- Dedicated support

#### 20.4 Enterprise Tier - "Healthcare" (Custom pricing)
- Unlimited beneficiaries
- All Professional features
- HL7 FHIR integration
- SSO/SAML
- Custom integrations
- SLA guarantees
- On-premise option
- Training and onboarding

---

## Implementation Roadmap

### Phase 1: MVP (Months 1-3)
- Core scheduling engine
- Basic restriction rules
- Coordinator/visitor roles
- Email notifications
- Web application
- Mobile-responsive design

### Phase 2: Enhanced Features (Months 4-6)
- iOS and Android apps
- SMS notifications
- Advanced restriction engine
- Calendar integrations
- Check-in/check-out
- Basic analytics

### Phase 3: Intelligence Layer (Months 7-9)
- AI-powered scheduling suggestions
- Fatigue management
- Wellbeing analytics
- Virtual visit integration
- Multi-language support

### Phase 4: Enterprise Ready (Months 10-12)
- EHR/EMR integrations
- HIPAA certification
- API and webhooks
- Enterprise SSO
- Advanced reporting
- Custom integrations

---

## Success Metrics

### Key Performance Indicators

| Metric | Target | Measurement |
|--------|--------|-------------|
| Visit scheduling success rate | >95% | Requested → Completed |
| No-show rate | <5% | Scheduled → No-show |
| Coordinator response time | <2 hours | Request → Decision |
| User satisfaction (NPS) | >50 | Regular surveys |
| App store rating | >4.5 stars | iOS/Android stores |
| Monthly active users | Growth 10%/month | Unique logins |
| Visitor engagement | >80% return | Repeat visit scheduling |

---

## Conclusion

VisitScheduler fills a critical gap in the market by combining the best features of medical scheduling software, appointment booking systems, caregiver coordination apps, and hospital visitor management into a single, purpose-built platform for proxy visit scheduling.

By focusing on the unique needs of those who cannot manage their own schedules—and the caregivers who support them—VisitScheduler delivers a compassionate, intelligent, and comprehensive solution that no existing tool adequately addresses.

---

## Research Sources

This specification was informed by analysis of the following platforms and resources:

**Medical Scheduling Software**
- CERTIFY Health, NexHealth, Acuity Scheduling, CareCloud
- Epic MyChart, athenahealth, Mend

**General Appointment Scheduling**
- Calendly, SimplyBook.me, Setmore, Square Appointments
- TimeTrade, Zoho Bookings

**Caregiver Coordination Apps**
- Caring Village, Lotsa Helping Hands, CaringBridge
- CareZone, Carely Family

**Home Care Scheduling**
- AxisCare, SUMO Scheduler, WellSky, Spectrum TeleTrack

**Hospital Visitor Management**
- Verkada, STOPware, Athena Security, VizitorApp

---

*Document Version: 1.0*
*Last Updated: January 2026*
