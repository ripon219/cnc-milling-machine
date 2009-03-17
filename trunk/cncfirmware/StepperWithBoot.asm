;********************************************************
;
;               Stepper Motor controller
;
;********************************************************

        list            p=pic16f877
        include         p16f877.inc

		radix dec

  		__CONFIG _CP_OFF & _XT_OSC & _PWRTE_ON  & _WDT_OFF & _BODEN_OFF & _LVP_ON

;;;;#DEFINE	Debug 1

RESET_V		EQU	0x0000
ISR_V		EQU	0x0004
OSC_FREQ	EQU	D'4000000'	


; Hardware config:
; PortA = Z Motor Control & Limit Switches
;  RA5 = \Limit-Zmax
;  RA4 = \Limit-Zmin
;  RA3 = Z Coil A
;  RA2 = Z Coil \A
;  RA1 = Z Coil B
;  RA0 = Z Coil \B 


; PortB = Start/Stop - Reserved
;  LCD Later / Menu input

; PortC = Limit Switches, I2C & Serial
;  RC7 = Serial RX
;  RC6 = Serial TX
;  RC5 = \Limit-Ymax
;  RC4 = I2C (Reserved)
;  RC3 = I2C (Reserved)
;  RC2 = \Limit-Ymin
;  RC1 = \Limit-Xmax
;  RC0 = \Limit-Xmin

; PortD = Motor Control
;  RD7 = Y Coil A
;  RD6 = Y Coil \A
;  RD5 = Y Coil B
;  RD4 = Y Coil \B
;  RD3 = X Coil A
;  RD2 = X Coil \A
;  RD1 = X Coil B
;  RD0 = X Coil \B


; PortE = Relay control & Start/Stop
;   RE2 = \Start-Stop
;   RE1 = Vacuum Relay
;   RE0 = Drill relay


; Step Sequence - CW: b'1010', b'0110', b'0101',b'1001'  - CCW: b'1001',b'0101',b'0110',b'1010'


; Constants
#define rcvbuffer_size 14
#define CR 0x0d
#define LF 0x0a

; MacroSendTable
;  Wraps the call to the sendTable subroutine in all of the appropriate
;    code to set and reset the proper PCLATH register so it does not
;    fly off to never-never land accidently when crossing pages
;
; To use it, just code MacroSendTable myTable

MacroSendTable	macro	tableLocation
		movlw		HIGH tableLocation	
		movwf		tableH
		movlw		LOW	tableLocation
		movwf		tableL
		lcall		sendTable
		movlw		HIGH $		; restore the proper PCLATH
		movwf		PCLATH
		endm

; all bits are numbered starting at 0 on the right - 76543210
; TRIS bits on indicates input, 0 indicates output

; Define EEPROM locations
	org 0x2100
			de "Vinces CNC Mill V0.1 - Vincent Greene", 0


pwrUpCfg
	; speed (delay counter)
			de	240		; xdelay
			de	240		; ydelay
			de	240		; zdelay
	
	; Steps per inch 
			de 	48		;xinch
			de 	48		;yinch
			de 	48		;zinch
	
	; holding current on/off )0/1
			de	0		;xhold
			de	0		;yhold
			de	0		;zhold
pwrUpCfgEnd



;****************  Label Definition  ********************
	cblock  h'20' ; bank 0
	
; Start of configuration - mirrored to EEPROM

	; speed (delay counter)
	xdelay
	ydelay
	zdelay
	
	; Steps per inch 
	xinch
	yinch
	zinch
	
	; holding current on/off )0/1
	xhold
	yhold
	zhold

	; stores current settings of motor coils A,\A,B,\B in low bits
	xcoils
	ycoils
	zcoils

	; Stores current location of an axis
	xlocation:2
	ylocation:2
	zlocation:2
	
	; steps to go in current movement
	xstepstogo
	ystepstogo
	zstepstogo
	
	; direction flag for current movement
	xdirection
	ydirection
	zdirection
	
	; motor state
	drillRelay
	vacuumRelay

	; misc varaibles	
	
	count1                          ;Wait counter
	count2                          ;Wait counter(for 1msec)
	bounceCount						; debounce Counter (used by debounce routine only)
	numparm
	offset
	temp2
	rcvbufoff
	rcvchar
	rcvbuffer:16
	B1 ; scratch area for ascii btye conversion
	tabofs ; Computed table offset used by DEFTAB macro
	echochar ; what to echo for keys entered
	axisOffset
	whichMotors; bit field used to determine whihc motors to move
	manualMode; 
	
	endc

	CBLOCK	0x70 ; used for bank-free variables
		sign ; ascii '-' if the value is negative
		digits: 5 ; five ascii digits for conversion to/from/decimal
		known_zero: 1 ; Fixed value zero for many calcs
		temp: 1 ; working variable for puts and 16b2acsii
		HI: 1 ; high byte for conversion
		LO: 1 ; low byte for conversion
		saveW ; temp storage for the W register
		tableH ; PCLATH for a table lookup
		tableL ; PCL for a table lookup
	ENDC


debounce macro register,bit,mbypass,release

	local	mstart, mloop

mstart
        btfsc   register,bit     ;ON (low)?
        goto    mbypass          ;No. Next

	ifndef	Debug
		; Wait 20 ms and check it again
		movlw	d'20'
		movwf	bounceCount
mloop	call	timer
        decfsz  bounceCount,f        ;count - 1 = 0 ?
        goto    mloop            ;No. Continue

		; 20 ms have passed - is it still low?
        btfsc   register,bit	 ;ON (low)?
        goto    mbypass          ;No. Next
	if release == 1
		btfss	register,bit	; Wait for a release
		goto	$-1
	endif
	else
		bsf		register,bit
		nop
		nop
		nop
		nop
		nop
		nop
		nop
		nop
	endif

	endm

	


;****************  Program Start  ***********************
;;BOOT        org     RESET_V         ;Reset Vector
;;BOOT        goto    init_V
        org     ISR_V           ;Interrupt Vector
		goto	interrupt


;**************** Interrupt Process *********************
interrupt
		clrf	INTCON
		retfie


; Initialization is at the end because page 0 is the best place for common subroutines
init_V	lgoto	init

; Subroutine SendTable
;  Dump a table address to the serial port (tables are NOT limited in size)
; table must end with a retlw 0
; Modifies varibles: tableH, tableL, temp
; input table location is: PCLATH in tableH, PCL in tableL - these will be modified by the process

; Macro MacroSendTable simplifies the calls by creating all of the needed setup and PCLATH reset code to
;   make the call completely safe.  Rather than calling SendTable directly, it is safer to use the macro

; It is WAY important that this is at the top of page 0 and 
;   not crossing any page boundarys

sendTable
	; get the character to send from the table
		call		PRIVATEloadTableByte
	; W contains the character - save it
		movwf		temp
	; Put the PCLATH back to what it should be (or we will jump to nowhere on next goto
		movlw		HIGH sendTable
		movwf		PCLATH

		movf		temp,w		; pull the byte to send back into w
		addlw		0	
		btfsc		STATUS,Z	; is it zero (end of table)?
		return					;    if so, return from subroutine

		movwf		TXREG		; transmit it
		movlw		TXSTA		; check status with indirect because it is in another page
        movwf		FSR         ; FSR <= TXSTA
txwait	btfss		INDF,1      ; check TRMT bit in TXSTA (FSR)
        goto 		txwait		; TXREG full  or TRMT = 0 
		
		; increment the table call location
		incfsz		tableL,f
		goto		sendTable
		incf		tableH,f
		goto		sendTable


		
; This ia calle by SendTable - don't use it directly or really ugly stuff will happen
PRIVATEloadTableByte
		movf		tableH,w
		movwf		PCLATH
		movf		tableL,w
		movwf		PCL			; this is the eqivilent to the call to the table
		return					; This return should never be executed - return
								;    will be to the location that dit the call the loadTableByte

; Subroutines go here so they are in page 0

; Subroutine ascii2byte 
; Reads exactly three ascii digits in the location pointed to by FSR,
;  FSR = 100s, FSR+1 = 10s, FSR+2 = 1s
; returns the value of the digits in variable numparm.  If the input is bad,
; variable FSR will contain the value 255 and numparm is undefined.
; If the input is good, numparm will contain the value of the input and 
; FSR will be zero.
; requires (and modifies):
;  temp2 - 1 byte
;  numparm - 1 byte

ascii2byte
		clrf		numparm
; First extract out the value part
;  First digit
		movlw		'0'				; sub ascii '0' = binary number
		subwf		INDF,w
		movwf		temp2
		
		movlw		9
		btfsc		STATUS,C
		goto		$+4
		lgoto		bad_input

		movf		temp2,w
		addwf		numparm,f

		; multiply by 10 
		bcf			STATUS,C
		rlf			numparm,f		; X 2
		bcf			STATUS,C
		rlf			numparm,f		; x 4
		bcf			STATUS,C
		rlf			numparm,f		; x 8
		movf		temp2,w
		addwf		numparm,f			; x 9
		addwf		numparm,f			; x 10

;  second digit
		incf		FSR,f
		movlw		'0'				; sub ascii '0' = binary number
		subwf		INDF,w
		movwf		temp2
		
		movlw		9
		btfss		STATUS,C
		goto		bad_input

		movf		temp2,w
		addwf		numparm,f
		movf		numparm,w
		movwf		temp2

		; multiply by 10 
		bcf			STATUS,C
		rlf			numparm,f		; X 2
		bcf			STATUS,C
		rlf			numparm,f		; x 4
		bcf			STATUS,C
		rlf			numparm,f		; x 8
		movf		temp2,w
		addwf		numparm,f			; x 9
		addwf		numparm,f			; x 10

;  third digit
		incf		FSR,f
		movlw		'0'				; sub ascii '0' = binary number
		subwf		INDF,w
		movwf		temp2
		
		movlw		9
		btfss		STATUS,C
		goto		bad_input

		movf		temp2,w
		addwf		numparm,f
		clrf		FSR
		return

; jump point to return a bad input response
bad_input
		movlw		255
		movwf		FSR
		return
		
        
; gets subroutine to receive cr terminated string into buffer
;   if echochar is non-zero, it will be echoed instead of entered char
gets      clrf		rcvbufoff
wait2     btfss     PIR1,RCIF      ; Check RCIF  bit in PIR1 register
          goto      wait2          ; RCREG empty or RCIF = 0
          movf      RCREG,w        ; RCREG full or RCIF = 1
          ; store the received char 
          movwf		rcvchar
          ; store the received char 
          movwf		rcvchar
          ; check W for a CR - if found, get out
          sublw		CR
          btfsc		STATUS,Z
          goto 		CRFound

			; echo either echochar (if non-zero), or actual char
		  movf		echochar,w
		  btfsc		STATUS,Z
		  movf		rcvchar,w
          movwf     TXREG
          
          movf		rcvchar,w
          ; check W for a LF - if found, skip it
          sublw		LF
          btfsc		STATUS,Z
          goto 		wait2
          ; Store the received char into the buffer
          movlw		rcvbuffer
          addwf		rcvbufoff,w
          movwf		FSR
          movf		rcvchar,w
          movwf		INDF
          incf		rcvbufoff,f
          ; check recv buffer offset for max size - if so, start over
          movf		rcvbufoff,w
          sublw		rcvbuffer_size
          btfss		STATUS,Z
          goto		wait2
         ; this is where we end up if at the end of the buffer
          goto		gets
          
		  ; when CR is found, write a 0 at buff position          
CRFound   movlw		rcvbuffer
          addwf		rcvbufoff,w
          movwf		FSR
          clrf		INDF
          
; At this point, the entered string is in the buffer
;  ready to be tested.
;  on return, rcvbufoff 
;   contains the length of the received string
          return      

; subroutine serout - send the byte in W to serial port
serout 	  movwf		TXREG
		  movlw		TXSTA
          movwf     FSR            ; FSR <= TXSTA
wait1     btfss     INDF,1         ; check TRMT bit in TXSTA (FSR)
          goto      wait1          ; TXREG full  or TRMT = 0 
          return

; puts subroutine 
;   this will dump the 
;   null-terminated string at FSR to the serial port
		  
puts      movf		FSR,w
		  movwf		temp
nextchar  movf		INDF,w
		  btfsc		STATUS,Z
		  return 
		  call		serout
		  incf		temp,f
		  movf		temp,w
		  movwf		FSR
		  goto		nextchar

;**********************************************************
; Convert time 1 byte to ASCII 2 bytes and send to display
; Input  : W - FSR points to 10s register
; Output : INDF 
;**********************************************************
BCD       movwf     B1             ; B1 = HHHH LLLL
          swapf     B1,w           ; W  = LLLL HHHH
          andlw     0x0f           ; Mask upper four bits 0000 HHHH
          addlw     0x30           ; convert to ASCII
          movwf		INDF           ; Store first digit
          movf      B1,w
          andlw     0x0f           ; w  = 0000 LLLL
          addlw     0x30           ; convert to ASCII
          incf		FSR,f
          movwf		INDF
          return


; 16 bits to Ascii 5 digits routine
; modified to write directly to digits in bank-free storage (0x70-0x7f)
; Input is HI and LO (destroyed!) output is digits+0 to digits+4

; by Rich Leggitt with tweaks by Scott Dattalo and bugfix by Dmitry Kiryashov and Nikolai Golovchenko
; given 16 bit data in HI and LO, extract decimal digits
; requires one Output register called temp, HI and LO are destroyed.
; 42 instructions and less than 269 (or 252 with known_zero) instructions executed

; use the s16b2ascii entry point for a signed -32767 to 32767 calculation
s16b2ascii
; added sign logic
		btfss	HI,7
		goto	positive
		; negative if we get here
		movlw	'-'
		movwf	sign
		comf	HI,f
		comf	LO,f
		
		movlw	1
		addwf	LO,f
		btfsc	STATUS,C
		incf	HI,f

		goto 	c16b2ascii

positive
		movlw	' '
		movwf	sign

; entry point for an unsigned 0-65535 calculation
c16b2ascii 
		clrf	temp
        goto $+2                ;[NG] was: skip
sub10k  incf temp,f
		movlw D'10000' & D'255'
        subwf LO,f

        rlf     known_zero,W
        sublw   (D'10000'>>8)+1    ;bugfix by Dmitry Kiryashov and Nikolai Golovchenko
        subwf   HI,F
        
        bc sub10k               ;9*7=63/8*7=56 inst in loop for 60900 (worst)
        movf	temp,w
        movwf	digits+0

        movlw D'10'
        movwf temp
add1K   decf temp,f
        movlw D'1000' & D'255'
        addwf LO,f

        rlf   known_zero,w
        addlw D'1000' >> 8
        addwf HI,f
        
        bnc add1K               ;9*10=90/8*10=80 inst in loop for 60900
        movf	temp,w
        movwf	digits+1

;Scott takes over here
        clrf  temp
        movlw D'100'
        goto $+2                ;[NG] was: skip
sub100
        incf  temp,f
        subwf LO,f
        skpnc                   ;[NG] was: skpc
        goto sub100

        decf  HI,f
        btfss HI,7      ;Check msb instead of carry for underflow.
        goto sub100     ;4 inst per loop to 200 then 7 per loop to 900. 
                        ;Total 64(?) in loop for worst case

;at this point, HI = 0xff, and  0 <= LO <= 99

        movf	temp,w
        movwf	digits+2

        movlw D'10'
        movwf temp
add10   decf temp,f
        addwf LO,f
        bnc add10               ;40 inst in loop for worst case.
        movf	temp,w
        movwf	digits+3
        movf	LO,w
        movwf	digits+4
		movlw	'0'
		addwf	digits+0,f		; convert to ASCII
		addwf	digits+1,f
		addwf	digits+2,f
		addwf	digits+3,f
		addwf	digits+4,f
        return

; Subroutine: calculateStep
;  in:   axisOffset = which axis to increment
;        xdirection+axisOffset = Direction (0=clockwise, 1=counterclockwise)
;  i/o:  xcoils+axisOffset = location of current motor coil controls
;  Temp Variables:
;        temp
;        FSR

calculateStep
	; Step Sequence - CW: b'1010', b'0110', b'0101',b'1001'  - CCW: b'1001',b'0101',b'0110',b'1010'

		movlw	xdirection
		addwf	axisOffset,w
		movwf	FSR				; FSR = xdirection+axisOffset
		movf	INDF,w	
		movwf	temp			; store the direction

		movlw	xcoils
		addwf	axisOffset,w
		movwf	FSR				; FSR = xcoils+axisOffset

		movf	INDF,w

        sublw   b'1010'         ;Check motor position
        bnz     drive2          ;Unmatch
        btfsc	temp,0			; Check direction
        goto    drive1          ;Set = CCW
        movlw   b'0110'         ;No. Set CW data
        goto    drive_end       ;Jump to write
drive1
        movlw   b'1001'			;Set CCW data
        goto    drive_end       ;Jump to write
;-------
drive2
		movf	INDF,w
        sublw   b'0110'			;Check motor position
        bnz     drive4          ;Unmatch
        btfsc	temp,0			; Check direction
        goto    drive3          ;Set = CCW
        movlw   b'0101'         ;No. Set CW data
        goto    drive_end       ;Jump to write
drive3
        movlw   b'1010'			;Set CCW data
        goto    drive_end       ;Jump to write
;-------
drive4
		movf	INDF,w
        sublw   b'0101'		    ;Check motor position
        bnz     drive6          ;Unmatch
        btfsc	temp,0			; Check direction
        goto    drive5          ;Set = CCW
        movlw   b'1001'         ;No. Set CW data
        goto    drive_end       ;Jump to write
drive5
        movlw   b'0110'			;Set CCW data
        goto    drive_end       ;Jump to write
;-------
drive6
		movf	INDF,w
        sublw   b'1001' 	    ;Check motor position
        bnz     drive8          ;Unmatch
        btfsc	temp,0			; Check direction
        goto    drive7          ;Set = CCW
        movlw   b'1010'         ;No. Set CW data
        goto    drive_end       ;Jump to write
drive7
        movlw   b'0101'			;Set CCW data
        goto    drive_end       ;Jump to write
;-------
drive8
        movlw   b'1010'  	   ;Compulsion setting - when nothing else matches - set first step

drive_end
		
        movwf   INDF	       ;Write PORTA
		return



;**********************************************************************
; Mainline - start of command processing
;**********************************************************************
ready     

		  MacroSendTable	ready_prompt_table
		  goto 		getcmdinp
		
ready_prompt_table
         dt			CR,LF,"$ "
	     retlw		0

getcmdinp
          call		gets
          
; main command processing goes right here          

cmdnext	  
		  movlw		'M'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		move_cmd

		  movlw		'C'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		calibrate_cmd

		  movlw		'S'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		set_cmd

		  movlw		'L'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		list_cmd

		  movlw		'H'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		holdcurrent_cmd

		  movlw		'D'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		drill_cmd

		  movlw		'V'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		vacuum_cmd

		  movlw		'B'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		save_cmd

		  movlw		'?'
		  subwf		rcvbuffer,w
		  btfsc		STATUS,Z
		  goto		help_cmd

; this is where we end up if the command is not in the table
bad_cmd
          
          movlw		rcvbuffer
          movwf		FSR
          lcall 	puts
		  movlw		HIGH $
		  movwf		PCLATH
          
		  MacroSendTable	badcmd_table
          goto		ready

badcmd_table
         dt			" - Invalid Command!",CR,LF
	     retlw		0


; Command implementations go here

holdcurrent_cmd
	; Evaluate common third character (on/off) first

		movlw		'0'
		subwf		rcvbuffer+2,w
		movwf		temp2			; temp2 is now the input char - '0'; 0 for off, 1 for on, somthing else for invalid
	
		sublw		1				; compare to 1
		btfss		STATUS,C
		goto 		bad_cmd			; if gt 1 it is bad

		; if you got here, temp2 has either a 0 or 1 (for on or off)
		; figure out which axis

		; same logic for the offset to the axis (x=0,y=1,z=2)
		
		movlw		'X'
		subwf		rcvbuffer+1,w
		movwf		offset			; offset has 0=x,1=y,2=z or something bad

		sublw		2				; compare to 2
		btfss		STATUS,C
		goto		bad_cmd			; if gt 2 it is bad

		; at this point, offset has the offset to the axis, temp2 has what to set it to

		movlw		xhold			; FSR = xhold + offset
		movwf		FSR
		movf		offset,w
		addwf		FSR,f

		movf		temp2,w
		movwf		INDF  			; ^FSR = temp2
		
		goto 		ready


drill_cmd

		movlw		'0'
		subwf		rcvbuffer+1,w
		movwf		temp2			; temp2 is now the input char - '0'; 0 for off, 1 for on, somthing else for invalid
	
		sublw		1				; compare to 1
		btfss		STATUS,C
		goto 		bad_cmd			; if gt 1 it is bad

		; if you got here, temp2 has either a 0 or 1 (for on or off)
		movf		temp2,w
		movwf		drillRelay

		bcf			PORTE,0		; turn it off
		btfsc		drillRelay,0
		bsf			PORTE,0		; turn it on
		
		goto 		ready


vacuum_cmd
		movlw		'0'
		subwf		rcvbuffer+1,w
		movwf		temp2			; temp2 is now the input char - '0'; 0 for off, 1 for on, somthing else for invalid
	
		sublw		1				; compare to 1
		btfss		STATUS,C
		goto 		bad_cmd			; if gt 1 it is bad

		; if you got here, temp2 has either a 0 or 1 (for on or off)
		movf		temp2,w
		movwf		vacuumRelay

		bcf			PORTE,1		; turn it off
		btfsc		vacuumRelay,0
		bsf			PORTE,1		; turn it on
				
		goto 		ready

calibrate_cmd
		; figure out which axis

		movlw		'X'
		subwf		rcvbuffer+1,w
		movwf		offset			; offset has 0=x,1=y,2=z or something bad

		sublw		2				; compare to 2
		btfss		STATUS,C
		goto		bad_cmd			; if gt 2 it is bad

		; at this point, offset has the index of the axis
		; multiply times two because the location is two bytes
		bcf			STATUS,C
		rlf			offset,f
		movlw		xlocation			; FSR = xlocation+ (offset*2)
		movwf		FSR
		movf		offset,w
		addwf		FSR,f

		clrf		INDF
		incf		FSR,f
		clrf		INDF
		
		goto 		ready


set_cmd
; Command processor for: " SDx+nnn<cr> = Set Delay for axis x +/-/= nnn delay",CR,LF
; and                    " SIx=nnn<cr> = Set Inch size on axis x to nnn steps",CR,LF

		movlw		rcvbuffer+4
		movwf		FSR
		lcall		ascii2byte
		btfsc		FSR,0
		goto		bad_cmd

		; figure out which axis

		movlw		'X'
		subwf		rcvbuffer+2,w
		movwf		offset			; offset has 0=x,1=y,2=z or something bad

		sublw		2				; compare to 2
		btfss		STATUS,C
		goto		bad_cmd			; if gt 2 it is bad

		; at this point, offset has the index of the axis

		; now figure out which value should be changed (put it in FSR)
		movf	rcvbuffer+1,w
		movwf	temp2

		movlw	'I'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	not_an_I
		
		movlw	xinch
		movwf	FSR
		goto	set_value

not_an_I
		movlw	'D'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	bad_cmd
				
		movlw	xdelay
		movwf	FSR

set_value

		movf	offset,w	; FSR = FSR + offset
		addwf	FSR,f

		; now deal with the value
		movf	rcvbuffer+3,w
		movwf	temp2

		movlw	'='
		subwf	temp2,w
		btfss	STATUS,Z
		goto	set_not_an_equal

	; process = numparm
		movf	numparm,w	; ^FSR = numparm
		movwf	INDF
		goto 	ready
		
set_not_an_equal
		movlw	'+'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	set_not_a_plus

	; process + numparm
		movf	numparm,w	; ^FSR += numparm
		addwf	INDF,f
		goto 	ready


set_not_a_plus
		movlw	'-'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	bad_cmd

	; process - numparm
		movf	numparm,w	; ^FSR -= numparm
		subwf	INDF,f
		goto 	ready


list_cmd
; Dumps current status like:
;(X) Axis: Location: (xlocation)  Delay: (xdelay) Steps Per Inch: (xinch)  Holding Current: (xhold)
;Y Axis: Location: nnnnn  Delay: xxx  Steps Per Inch: xxx  Holding Current: 0
;Z Axis: Location: nnnnn  Delay: xxx  Steps Per Inch: xxx  Holding Current: 0
;Drill=0
;Vacuum=0

		clrf		axisOffset

list_next_axis
		movlw		3
		subwf		axisOffset,w
		btfsc		STATUS,C
		goto		list_axis_done

		movlw		'X'
		addwf		axisOffset,w	; ' results in w = 'X','Y',or 'Z'

		lcall		serout			; print the axis
		movlw		HIGH $
		movwf		PCLATH
		
		MacroSendTable	location_table
          lgoto		list_part2

location_table
		dt			" Axis: Location: "
		retlw		0

list_part2

		; dump the location
		movf		axisOffset,w
		movwf		FSR
		bcf			STATUS,C
		rlf			FSR,f			; FSR = axisOffset*2
		movlw		xlocation		; FSR += xlocation
		addwf		FSR,f

		movf		INDF,w
		movwf		HI

		incf		FSR,f
		movf		INDF,w
		movwf		LO

		lcall		s16b2ascii	; converts signed value to ascii in sign + digits:5
		; no PCLATH reset because next instruction is long

		movlw		sign
		movwf		FSR
		lcall		puts		; show the digits on the screen
		; no PCLATH reset because next instruction is long
		
		
		MacroSendTable	delay_table
         lgoto		list_part3

delay_table
		dt			"  Delay: "
		retlw		0

list_part3


		; dump the delay
		movlw		xdelay
		movwf		FSR
		movf		axisOffset,w		; FSR = xlocation+axisOffset
		addwf		FSR,f

		clrf		HI

		movf		INDF,w
		movwf		LO

		lcall		c16b2ascii	; converts to ascii in digits:5
		; no PCLATH reset because next instruction is long

		movlw		digits+2	; only use last three digits
		movwf		FSR
		lcall		puts		; show the digits on the screen
		; no PCLATH reset because next instruction is long


		MacroSendTable	inch_table
         lgoto		list_part4

inch_table
		dt			"  Steps per Inch: "
		retlw		0

list_part4

		; dump the steps per inch
		movlw		xinch
		movwf		FSR
		movf		axisOffset,w		; FSR = xlocation+axisOffset
		addwf		FSR,f

		clrf		HI

		movf		INDF,w
		movwf		LO

		lcall		c16b2ascii	; converts to ascii in digits:5
		; no PCLATH reset because next instruction is long

		movlw		digits+2	; only use last three digits
		movwf		FSR
		lcall		puts		; show the digits on the screen
		; no PCLATH reset because next instruction is long


		MacroSendTable	holding_table
         lgoto		list_part5

holding_table
		dt			"  Holding Current: "
		retlw		0

list_part5
		; Dump the holding current setting
		movlw		xhold
		movwf		FSR
		movf		axisOffset,w		; FSR = xlocation+axisOffset
		addwf		FSR,f

		movlw		'0'					; convert to ASCII number
		addwf		INDF,w
		lcall		serout
		; no PCLATH reset because next instruction is long

		movlw		CR
		lcall		serout
		; no PCLATH reset because next instruction is long
		movlw		LF
		lcall		serout
		; no PCLATH reset because next instruction is long

		incf		axisOffset,f
		lgoto		list_next_axis

list_axis_done

		MacroSendTable	drill_table
         goto		list_part6

drill_table
		dt			"Drill="
		retlw		0

list_part6
		; Dump the drill relay status
		movlw		'0'					; convert to ASCII number
		addwf		drillRelay,w
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	vacuum_table
         lgoto		list_part7

vacuum_table
		dt			CR,LF,"Vacuum="
		retlw		0

list_part7
		; Dump the vacuum relay status
		movlw		'0'					; convert to ASCII number
		addwf		vacuumRelay,w
		lcall		serout
		; no PCLATH reset because next instruction is long

dump_limits
; Dump X axis limit switches

		MacroSendTable	limit_table
		lgoto		list_part8

limit_table
		dt			CR,LF,"Limit Switch: "
		retlw		0

list_part8
		movlw		'X'
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	min_table
		lgoto		list_part9

min_table
		dt			"-axis min="
		retlw		0

list_part9
		movlw		'0'
		btfss		PORTC,0
		movlw		'1'
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	max_table
		lgoto		list_part10

max_table
		dt			" max="
		retlw		0

list_part10
		movlw		'0'
		btfss		PORTC,1
		movlw		'1'
		lcall		serout
		; no PCLATH reset because next instruction is long
		
; Dump Y axis limit switches

		MacroSendTable	limit_table

		movlw		'Y'
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	min_table

		movlw		'0'
		btfss		PORTC,2
		movlw		'1'
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	max_table
		movlw		'0'
		btfss		PORTC,5
		movlw		'1'
		lcall		serout
		; no PCLATH reset because next instruction is long
		
; Dump Z axis limit switches

		MacroSendTable	limit_table

		movlw		'Z'
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	min_table

		movlw		'0'
		btfss		PORTA,4
		movlw		'1'
		lcall		serout
		; no PCLATH reset because next instruction is long

		MacroSendTable	max_table
		movlw		'0'
		btfss		PORTA,5
		movlw		'1'
		lcall		serout
		; no PCLATH reset because next instruction is long

		lgoto	ready

manual_cmd
; Implements: MN - Manual axis movement
;
; This starts moving any axis in any direction based on directional keystrokes.  Basically
;  each keystroke moves the specific axis and direction exactly one inch or until the 
;  next keystroke or a limit switch is activated.

		bsf			manualMode,0	; set manual mode on

		MacroSendTable	manual_table
         lgoto		manual_nextkey

manual_table
		dt			"Use the folowing motion keys:",CR,LF,CR,LF
		dt			"  Q(Z+)  W(Y+)",CR,LF
		dt			"  A(X-)  S(Stop)   D(X+)",CR,LF
		dt			"  Z(Z-)  X(Y-)",CR,LF,CR,LF
		dt			"Press <spx> to exit manual mode",CR,LF
		retlw		0

manual_nextkey
	; get the next pressed key
       btfss     	PIR1,RCIF      ; Check RCIF  bit in PIR1 register
       goto      	manual_nextkey ; RCREG empty or RCIF = 0

manual_keypress
	; process the pressed key
       movf      	RCREG,w        ; RCREG full or RCIF = 1
    ; store the received char 
       movwf		rcvchar

		movlw		'S'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_nextkey	; S = stop and process next key

		movlw		'Q'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_zplus

		movlw		'Z'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_zminus

		movlw		'W'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_yplus

		movlw		'X'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_yminus
		
		movlw		'A'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_xplus

		movlw		'D'
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		manual_xminus

		movlw		' '
		subwf		rcvchar,w
		btfsc		STATUS,Z
		goto		ready

		goto		manual_nextkey


; Now the specific directional moves

manual_zplus
		movlw		0
		movwf		zdirection
		movf		zinch,w
		movwf		zstepstogo
		goto		move_the_steppers

manual_zminus
		movlw		1
		movwf		zdirection
		movf		zinch,w
		movwf		zstepstogo
		goto		move_the_steppers

manual_yplus
		movlw		0
		movwf		ydirection
		movf		yinch,w
		movwf		ystepstogo
		goto		move_the_steppers

manual_yminus
		movlw		1
		movwf		ydirection
		movf		yinch,w
		movwf		ystepstogo
		goto		move_the_steppers

manual_xplus
		movlw		0
		movwf		xdirection
		movf		xinch,w
		movwf		xstepstogo
		goto		move_the_steppers

manual_xminus
		movlw		1
		movwf		xdirection
		movf		xinch,w
		movwf		xstepstogo
		goto		move_the_steppers


move_cmd
;              012345678901
; Implements:  Mx+nnn[y+mmm]<cr> = Move axis x +/- nnn steps
;  the second axis is optional
;  and "MN<cr>", which goes to manual single axis controls

; First clear the axis movement counters
		clrf		xstepstogo
		clrf		ystepstogo
		clrf		zstepstogo
; and the direction flags (0=forward, 1=backwards)
		clrf		xdirection
		clrf		ydirection
		clrf		zdirection

; Figure out if it is a fixed move or manual move
		clrf		manualMode

		movlw		'N'
		subwf		rcvbuffer+1,w
		btfsc		STATUS,Z
		goto		manual_cmd

; Start by interpreting the number for the first axis
		movlw		rcvbuffer+3
		movwf		FSR
		lcall		ascii2byte
		btfsc		FSR,0
		goto		bad_cmd

		; figure out which axis

		movlw		'X'
		subwf		rcvbuffer+1,w
		movwf		offset			; offset has 0=x,1=y,2=z or something bad

		sublw		2				; compare to 2
		btfss		STATUS,C
		goto		bad_cmd			; if gt 2 it is bad

		; at this point, offset has the index of the axis
		; now figure out the direction
		movlw	xdirection
		addwf	offset,w
		movwf	FSR					; FSR = xdirection+offset

		movf	rcvbuffer+2,w
		movwf	temp2

		movlw	'+'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	move_not_plus
		clrf	INDF				; if '+', direction flag is 0
		goto	direction_done

move_not_plus
		movlw	'-'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	bad_cmd				; if not '-' either, it is bad command

		; direction is '-'
		movlw	1	
		movwf	INDF				; if '-', direction is 1

direction_done
		; now set the appropriate step counter
		movlw	xstepstogo
		addwf	offset,w
		movwf	FSR					; FSR = xstepstogo + offset
		
		movf	numparm,w
		movwf	INDF
		

		; Check for a second axis in the command and process it if needed

		movf		rcvbuffer+6,f
		btfsc		STATUS,Z	
		goto		move_the_steppers	; If the command ends with null, get out		

		; If we get here, there is a second axis to process

		movlw		rcvbuffer+8
		movwf		FSR
		lcall		ascii2byte
		btfsc		FSR,0
		goto		bad_cmd

		; figure out which axis

		movlw		'X'
		subwf		rcvbuffer+6,w
		movwf		offset			; offset has 0=x,1=y,2=z or something bad

		sublw		2				; compare to 2
		btfss		STATUS,C
		goto		bad_cmd			; if gt 2 it is bad

		; at this point, offset has the index of the axis
		; now figure out the direction
		movlw	xdirection
		addwf	offset,w
		movwf	FSR					; FSR = xdirection+offset

		movf	rcvbuffer+7,w
		movwf	temp2

		movlw	'+'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	move_not_plus2
		clrf	INDF				; if '+', direction flag is 0
		goto	direction_done2

move_not_plus2
		movlw	'-'
		subwf	temp2,w
		btfss	STATUS,Z
		goto	bad_cmd				; if not '-' either, it is bad command

		; direction is '-'
		movlw	1	
		movwf	INDF				; if '-', direction is 1

direction_done2
		; now set the appropriate step counter
		movlw	xstepstogo
		addwf	offset,w
		movwf	FSR					; FSR = xstepstogo + offset
		
		movf	numparm,w
		movwf	INDF

move_the_steppers
		clrf		count1		; start with no delay
		clrf		whichMotors ; start with no motors moving 0=PORTD, 2=PORTA
; process each axis

		movf		xstepstogo,w
		bz			calcYaxis

		; process X axis - it has some steps to do
		decf		xstepstogo,f
		bsf			whichMotors,0

		; update the location

		btfsc		xdirection,0
		goto		decrx

		movlw		1
		addwf		xlocation+1,f
		btfsc		STATUS,C
		incf		xlocation+0,f
		goto		calcx

decrx	
		movlw		1
		subwf		xlocation+1,f
		btfss		STATUS,C
		decf		xlocation+0,f


calcx
		; calculate the values for the motor coils
		movlw		0
		movwf		axisOffset
		lcall		calculateStep
		movlw		HIGH $
		movwf		PCLATH
		
		movf		xdelay,w
		movwf		count1				; store the delay for x axis

calcYaxis
		movf		ystepstogo,w
		bz			calcZaxis

		; process Y axis - it has steps to go
		decf		ystepstogo,f
		bsf			whichMotors,0

		; update the location

		btfsc		ydirection,0
		goto		decry

		movlw		1
		addwf		ylocation+1,f
		btfsc		STATUS,C
		incf		ylocation+0,f
		goto		calcy

decry	
		movlw		1
		subwf		ylocation+1,f
		btfss		STATUS,C
		decf		ylocation+0,f


calcy
		; calculate the values for the motor coils
		movlw		1
		movwf		axisOffset
		lcall		calculateStep
		movlw		HIGH $
		movwf		PCLATH
		
		movf		ydelay,w
		subwf		count1,w
		btfss		STATUS,C
		goto		calcZaxis			; this means count1 is larger than ydelay

		movf		ydelay,w
		movwf		count1				; store the delay for y axis

calcZaxis
		movf		zstepstogo,w
		bz			doMotors

		; process Z axis - it has steps to go
		decf		zstepstogo,f
		bsf			whichMotors,2

		; update the location

		btfsc		zdirection,0
		goto		decrz

		movlw		1
		addwf		zlocation+1,f
		btfsc		STATUS,C
		incf		zlocation+0,f
		goto		calcz

decrz	
		movlw		1
		subwf		zlocation+1,f
		btfss		STATUS,C
		decf		zlocation+0,f

calcz
		; calculate the values for the motor coils
		movlw		2
		movwf		axisOffset
		lcall		calculateStep
		movlw		HIGH $
		movwf		PCLATH
		
		movf		zdelay,w
		subwf		count1,w
		btfsc		STATUS,C
		goto		doMotors			; this means count1 is larger than zdelay

		movf		zdelay,w
		movwf		count1				; store the delay for z axis

doMotors
		; move the actual motors and wait the delay time
		; probably should check for interupts here (serial, stop button, and limit switches)

; So, how to figure out which motors to move???

; Move the ZAxis motor


		movf		whichMotors,f
		btfsc		STATUS,Z
		goto		doneMoving

		btfss		whichMotors,2		; move z axis?
		goto		xyMotors

		; set PORTA correctly

		movf		zcoils,w
		movwf		PORTA

xyMotors

		btfss		whichMotors,0		; move x or y axis?
		goto		pause

		movf		ycoils,w
		movwf		temp2
		swapf		temp2,f			; y in high bits

		movf		xcoils,w
		iorwf		temp2,w			; x in low bits

		movwf		PORTD
		
pause
        movf   	count1,f
		bz		checkHold

	ifndef	Debug
loop    call    timer           ;Wait 1msec
        decfsz  count1,f        ;count - 1 = 0 ?
        goto    loop            ;No. Continue
	else
		nop
		nop
		nop
	endif


		goto	checkHold

; Putting the simple timer subroutine here so it is a short call
;*************  1msec Timer Subroutine  *****************
timer
	ifndef Debug
        movlw   d'200'          ;Set loop count
        movwf   count2          ;Save loop count
tmlp    nop                     ;Time adjust
        nop                     ;Time adjust
        decfsz  count2,f        ;count - 1 = 0 ?
        goto    tmlp            ;No. Continue
	else
		nop	
		nop
		nop
		nop
		nop	
		nop
	endif
        return                  ;Yes. Count end



checkHold
		; check the hold flags, if they are 0, turn off that motor
		btfss		zhold,0
		; z holding current is off, clear PORTA
		clrf		PORTA

		btfsc		xhold,0
		goto		checkyhold
	
		; x holding current is off
		movlw		b'11110000'
		andwf		PORTD,f

checkyhold
		btfsc		yhold,0
		goto		check_limits
	
		; y holding current is off
		movlw		b'00001111'
		andwf		PORTD,f

check_keypress
		btfss		PIR1,RCIF		; Check for a serial receive
		goto		check_limits

		; in manual mode, go back to key processing
		btfsc		manualMode,0
		goto		manual_keypress

		; any keypress aborts in programmed move mode

		; read and discard the character
          movf      RCREG,w
          ; store the received char 
          movwf		rcvchar

		MacroSendTable	abort_table
		lgoto		doneMoving

abort_table
		dt			CR,LF,"SERIAL RECEIVE ABORT",CR,LF
		retlw		0

check_limits

		btfss		PORTC,0
		goto		limit_error
		
		btfss		PORTC,1
		goto		limit_error
	
		btfss		PORTC,2
		goto		limit_error

		btfss		PORTC,5
		goto		limit_error

		btfss		PORTA,4
		goto		limit_error
		
		btfss		PORTA,5
		goto		limit_error

		goto		move_the_steppers


limit_error
	; if we get here, we have to stop stepping because a limit switch is activated

		MacroSendTable	limit_error_table
		goto		doneMoving

limit_error_table
		dt			CR,LF,"LIMIT ERROR",CR,LF
		retlw		0

doneMoving
		btfsc	manualMode,0
		goto	manual_nextkey

		goto 	ready


save_cmd
	; EEPROM Read from power-up config - uses temp as the memory location

	movlw	pwrUpCfgEnd-pwrUpCfg	; store them number of loops
	movwf	temp

	BANKSEL	EEADR
	movlw	pwrUpCfg-0x2100
	movwf	EEADR			; store the start of EEPROM to write
	movlw	H'20'			; store the start of RAM to read
	movwf	FSR
	
	bcf		INTCON, GIE	; no interrupts allowed

eepromwrite
	BANKSEL	H'20'		; select bank 0 for the ram
	movf	INDF,w		; indirect load of the data to write
	BANKSEL	EEDATA
	movwf	EEDATA		
	BANKSEL	PIR2
	bcf		PIR2,EEIF
	BANKSEL	EECON1
	bcf		EECON1, EEPGD   ; not program memory
	bsf		EECON1, WREN    ; Enable writes
	; Required sequence
	movlw	55h
	movwf	EECON2
	movlw	0AAh
	movwf	EECON2
	bsf		EECON1,WR
	; end required sequence
	bcf		EECON1,WREN
	BANKSEL	PIR2
pollWR
	btfss	PIR2,EEIF	; loop until the write is complete
	goto	pollWR

	incf	FSR,f		; address of ram
	BANKSEL	EEADR
	incf	EEADR,f		; address of rom
	decfsz	temp,f	; loop loopvar times 
	goto 	eepromwrite

	; done writing

	BANKSEL	H'20'		; back to bank 0

	goto	ready


notdone   movlw		rcvbuffer
          movwf		FSR
          lcall 		puts
          
		  MacroSendTable	notdone_table
		  goto		help_cmd

notdone_table
         dt			" - Command not implemented yet!",CR,LF
	     retlw		0

       
help_cmd  
		  MacroSendTable	help_table
          lgoto		ready

help_table
		dt			"Commands:",CR,LF
		dt			" MN<cr> = Manual Control",CR,LF
		dt			" Mx+nnn[y+mmm]<cr> = Move axis x +/- nnn steps",CR,LF
		dt			" Cx<cr> = Calibrate axis x (reset to 0)",CR,LF
		dt			" SDx+nnn<cr> = Set Delay for axis x +/-/= nnn delay",CR,LF
		dt			" SIx=nnn<cr> = Set Inch size on axis x to nnn steps",CR,LF
		dt			" L<cr> = Lists current settings and location",CR,LF
		dt			" Hx0 = Holding current for axis x Off (0) or On (1)",CR,LF
		dt			" D0<cr> = Drill Off (0) or On (1)",CR,LF
		dt			" V0<cr> = Vacuum Off (0) or On (1)",CR,LF
		dt			" B<cr> = Backup startup config",CR,LF
		dt 			" ?<cr> = Dump this message",CR,LF
		retlw		0



;****************  Initial Process  *********************
; Note that this is at the end because page 0 is the best place for subroutines
init
		clrf		known_zero ; this MUST be zero 

; Initialize Bank 1 registers 

		BANKSEL		TRISA
		movlw		b'10000000'
		movwf		OPTION_REG

		movlw		b'110000'
		movwf		TRISA

		movlw		b'11111111'
		movwf		TRISB

		movlw		b'11111111'
		movwf		TRISC

		movlw		b'00000000'
		movwf		TRISD

		movlw		b'100'
		movwf		TRISE

		clrf		PIE1
		clrf		PIE2

		movlw		0x06
		movwf		ADCON1


;	Bank 0 initialization

		BANKSEL		PORTA		; back to bank 0

		clrf		INTCON

		clrf		PIR1
		clrf		PIR2

		clrf		ADCON0

; clear all of page 0 RAM  X'20' to x'7F'

		BANKSEL		H'20'
		movlw		H'7F'
		movwf		FSR
		
ramclrloop
		clrf		INDF
		decf		FSR,f
		
		movf		FSR,w
		sublw		H'20'
		bnz			ramclrloop

; load config area from EEPROM

loadConfig
	; EEPROM Read from power-up config - uses temp as the memory location

	movlw	pwrUpCfgEnd-pwrUpCfg	; store the number of loops
	movwf	temp

	BANKSEL	EEADR
	movlw	pwrUpCfg-0x2100
	movwf	EEADR			; store the start of EEPROM to read
	movlw	H'20' 			; store the start of RAM to write
	movwf	FSR
	
eepromread
	BANKSEL	EECON1
	bcf		EECON1,EEPGD; Select EEPROM
	bsf		EECON1, RD	; EEPROM Read
	BANKSEL	EEDATA
	movf	EEDATA, W	; W = the data read
	movwf	INDF		; store the EEPROM byte to ram
	incf	FSR,f		; address of ram
	incf	EEADR,f		; address of rom
	decfsz	temp,f		; loop temp times 
	goto 	eepromread


; Serial port setup

		BANKSEL		TXSTA
        clrf      	TXSTA          ; 8 bits data ,no,1 stop
		bsf			TXSTA,BRGH	   ; enable high speed BRG
        bsf       	TXSTA,TXEN     ; Transmit enable

		BANKSEL		SPBRG
        movlw     	12             ; BAUD rate 19.2k
        movwf     	SPBRG

		BANKSEL		RCSTA
  		bsf       	RCSTA,SPEN     ; Asynchronous serial port enable
        bsf       	RCSTA,CREN     ; continuous receive

		BANKSEL		H'20'

    	goto		greet

greet     
		MacroSendTable	greet_table
		lgoto		ready

greet_table
	      dt		CR,LF,"Vince's CNC MILL V0.1",CR,LF
	      retlw		0


;********************************************************
;             END of Stepper Motor controller
;********************************************************


;-----------------------------------------------------------------------------
;Constants

BAUD_CONSTANT	EQU	d'12'	;Constant for baud generator for 2400 baud
				;Fosc is 4MHz

;-----------------------------------------------------------------------------
;Variables in bank0

		CBLOCK	0x20
		AddressH:	1	;flash program memory address high byte
		AddressL:	1	;flash program memory address low byte
		NumWords:	1	;number of words in line of hex file
		Checksum:	1	;byte to hold checksum of incoming data
		Counter:	1	;to count words being saved or programmed
		TestByte:	1	;byte to show reset vector code received
		HexByte:	1	;byte from 2 incoming ascii characters
		Expect:		1   ;next byte to watch for on Serial port
		TmrCycle:	1	; count down for Timer1 overflows
		toffset:		1   ; offset within the table
		DataPointer:	1	;pointer to data in buffer
		DataArray:	0x40	;buffer for storing incoming data
		ENDC

;-----------------------------------------------------------------------------
;Macros to select the register bank
;Many bank changes can be optimised when only one STATUS bit changes

Bank0		MACRO			;macro to select data RAM bank 0
		bcf	STATUS,RP0
		bcf	STATUS,RP1
		ENDM

Bank1		MACRO			;macro to select data RAM bank 1
		bsf	STATUS,RP0
		bcf	STATUS,RP1
		ENDM

Bank2		MACRO			;macro to select data RAM bank 2
		bcf	STATUS,RP0
		bsf	STATUS,RP1
		ENDM

Bank3		MACRO			;macro to select data RAM bank 3
		bsf	STATUS,RP0
		bsf	STATUS,RP1
		ENDM

;=============================================================================
;Reset vector code

		ORG	0x0000 

ResetVector:	movlw	high Main
		movwf	PCLATH		;set page bits for page3
  		goto    Main		;go to boot loader

;=============================================================================
;Start of boot code in upper memory traps accidental entry into boot code area

		ORG	0x1e00		;Use last part of page3 for PIC16F876/7
;		ORG	0x0f20		;Use last part of page1 for PIC16F873/4
;		ORG	0x0720		;Use last part of page0 for PIC16F870/1

StartOfBoot:	movlw	high TrapError	;trap if execution runs into boot code
		movwf	PCLATH		;set correct page
TrapError:	goto	TrapError	;trap error and wait for reset

;-----------------------------------------------------------------------------
;Relocated user reset code to jump to start of user code
;Must be in bank0 before jumping to this routine

StartUserCode:	clrf	PCLATH		;set correct page for reset condition 
		goto    init_V ;; vector to Stepper code
;;		nop			;relocated user code replaces this nop
		nop			;relocated user code replaces this nop
		nop			;relocated user code replaces this nop
		nop			;relocated user code replaces this nop

		; Print a '!' if there is no goto in user reset code
		movlw	'!'
		call	SerialTransmit
		
		movlw	high TrapError1	;trap if no goto in user reset code
		movwf	PCLATH		;set correct page
TrapError1:	goto	TrapError1	;trap error and wait for reset

;-----------------------------------------------------------------------------
;Program memory location to show whether valid code has been programmed

CodeStatus:	;;DA	0x3fff		;0 for valid code, 0x3fff for no code
				DA	0x0000		;;; BOOT - for stepper combined code
;-----------------------------------------------------------------------------
;Main boot code routine
;Tests to see if a load should occur and if valid user code exists

Main:		Bank0			;change to bank0 in case of soft reset

		call 	SerialSetup
		
		movlw	'B'
		call 	SerialTransmit
		movlw	'O'
		call 	SerialTransmit
		movlw	'O'
		call 	SerialTransmit
		movlw	'T'
		call 	SerialTransmit

		; setup Timer1 to count instructions
		; after 524280 instructions (1/2 second) the PIR,TMR1IF flag is set
		Bank0
		movlw	6
		movwf	TmrCycle
		
		Bank1
		bcf		PIE1,TMR1IE
		Bank0
		movlw 	B'00110001'
		movwf	T1CON
		bcf		PIR1,TMR1IF
		clrf	TMR1H
		clrf	TMR1L
		

		clrf	toffset
nxchar
		call	prog_table
		movwf	Expect		; set the next char to expect
		movf	Expect,w	; test for 0 at end
		btfsc	STATUS,Z
		goto	Loader		; we get here if they all match
		
nx1		btfss	PIR1,TMR1IF ; Timer overflow?
		goto 	chkser		; no overflow - check the serial port
		decf	TmrCycle,f
		btfsc	STATUS,Z	; if it is zero, time is up
		goto 	chkstatus
		
		bcf		PIR1,TMR1IF
		clrf	TMR1H
		clrf	TMR1L
		bsf		T1CON,TMR1ON	; turn on the timer
		

chkser	btfss	PIR1,RCIF	;check if data received
		goto	nx1
		movf	RCREG,W		;get received data into W
		movwf	TXREG	; SEND IT (TEMP)
		subwf	Expect,w
		btfss	STATUS,Z	; does it match?
		goto	chkstatus	; mismatch sends it to user code
		incf	toffset,f
		goto 	nxchar

; table is here so it doesn't cross a 256 byte boundry		
prog_table
		movf	toffset,w
		addwf	PCL,f
		dt		"PROG",0
		
		
chkstatus		
		Bank0
		bcf		T1CON,TMR1ON	; turn off the timer
		call	LoadStatusAddr	;load address of CodeStatus word
		call	FlashRead	;read data at CodeStatus location
		
		Bank2			;change from bank3 to bank2
		movf	EEDATA,F	;set Z flag if data is zero
		Bank0			;change from bank2 to bank0
		btfsc	STATUS,Z	;test Z flag
		goto	StartUserCode	;if zero then run user code

		; Print a '?' if there is no user program
		movlw	'?'
		call	SerialTransmit

		; loop until something received on serial then start over
		btfss	PIR1,RCIF	;check if data received
		goto	$-1
		goto 	Main

;-----------------------------------------------------------------------------
;Start of routine to load and program new code

Loader:		clrf	TestByte	;indicate no reset vector code yet

		call	LoadStatusAddr	;load address of CodeStatus word
		movlw	0x3f		;load data to indicate no program
		movwf	EEDATH
		movlw	0xff		;load data to indicate no program
		movwf	EEDATA
		call	FlashWrite	;write new CodeStatus word

		call	SerialSetup	;set up serial port
		
		; Print . to start the loader
		movlw	'.'
		call	SerialTransmit

;-----------------------------------------------------------------------------
;Get new line of hex file starting with ':'
;Get first 8 bytes after ':' and extract address and number of bytes

GetNewLine:	call	SerialReceive	;get new byte from serial port
		xorlw	':'		;check if ':' received
		btfss	STATUS,Z
		goto	GetNewLine	;if not then wait for next byte

		clrf	Checksum	;start with checksum zero

		call	GetHexByte	;get number of program data bytes in line
		andlw	0x1F		;limit number in case of error in file
		movwf	NumWords
		bcf	STATUS,C
		rrf	NumWords,F	;divide by 2 to get number of words

		call	GetHexByte	;get upper half of program start address
		movwf	AddressH

		call	GetHexByte	;get lower half of program start address
		movwf	AddressL

		bcf	STATUS,C
		rrf	AddressH,F	;divide address by 2 to get word address
		rrf	AddressL,F

		call	GetHexByte	;get record type
		xorlw	0x01
		btfsc	STATUS,Z	;check if end of file record (0x01)
		goto	FileDone	;if end of file then all done

		movf	HexByte,W
		xorlw	0x00
		btfss	STATUS,Z	;check if regular line record (0x00)
		goto	LineDone	;if not then ignore line and send '.'

		movlw	0xe0
		addwf	AddressH,W	;check if address < 0x2000
		btfsc	STATUS,C	;which is ID locations and config bits
		goto	LineDone	;if so then ignore line and send '.'

;-----------------------------------------------------------------------------
;Get data bytes and checksum from line of hex file

		movlw	DataArray
		movwf	FSR		;set pointer to start of array
		movf	NumWords,W
		movwf	Counter		;set counter to number of words

GetData:	call	GetHexByte	;get low data byte
		movwf	INDF		;save in array
		incf	FSR,F		;point to high byte

		call	GetHexByte	;get high data byte
		movwf	INDF		;save in array
		incf	FSR,F		;point to next low byte

		decfsz	Counter,F
		goto	GetData

		call	GetHexByte	;get checksum
		movf	Checksum,W	;check if checksum correct
		btfss	STATUS,Z
		goto	ErrorMessage

;-----------------------------------------------------------------------------
;Get saved data one word at a time to program into flash 

		movlw	DataArray
		movwf	FSR		;point to start of array
		movf	NumWords,W
		movwf	Counter		;set counter to half number of bytes

;-----------------------------------------------------------------------------
;Check if address is in reset code area

CheckAddress:	movf	AddressH,W	;checking for boot location code
		btfss	STATUS,Z	;test if AddressH is zero 
		goto	CheckAddress1	;if not go check if reset code received

		movlw	0xfc	
		addwf	AddressL,W	;add 0xfc (-4) to address
		btfsc	STATUS,C	;no carry means address < 4
		goto	CheckAddress1	;if not go check if reset code received

		bsf	TestByte,0	;show that reset vector code received
		movf	AddressL,W	;relocate addresses 0-3 to new location
		addlw	low (StartUserCode + 1) ;add low address to new location
		Bank2			;change from bank0 to bank2
		movwf	EEADR		;load new low address
		movlw	high (StartUserCode + 1) ;get new location high address
		movwf	EEADRH		;load high address
		goto	LoadData	;go get data byte and program into flash

;-----------------------------------------------------------------------------
;Check if reset code has been received
;Check if address is too high and conflicts with boot loader

CheckAddress1:	btfss	TestByte,0	;check if reset vector code received first
		goto	ErrorMessage	;if not then error

		movlw	high StartOfBoot ;get high byte of address
		subwf	AddressH,W
		btfss	STATUS,C	;test if less than boot code address 
		goto	LoadAddress	;yes so continue with write
		btfss	STATUS,Z	;test if equal to boot code address 
		goto	ErrorMessage	;no so error in high byte of address

		movlw	low StartOfBoot	;get low byte of address
		subwf	AddressL,W
		btfsc	STATUS,C	;test if less than boot code address 
		goto	ErrorMessage	;no so error in address

;-----------------------------------------------------------------------------
;Load address and data and write data into flash

LoadAddress:	movf	AddressH,W	;get high address
		Bank2			;change from bank0 to bank2
		movwf	EEADRH		;load high address
		Bank0			;change from bank2 to bank0
		movf	AddressL,W	;get low address
		Bank2			;change from bank0 to bank2
		movwf	EEADR		;load low address

LoadData:	movf	INDF,W		;get low byte from array
		movwf	EEDATA		;load low byte
		incf	FSR,F		;point to high data byte
		movf	INDF,W		;get high byte from array
		movwf	EEDATH		;load high byte
		incf	FSR,F		;point to next low data byte

		call	FlashWrite	;write data to program memory

		Bank0			;change from bank3 to bank0
		incfsz	AddressL,F	;increment low address byte
		goto	CheckLineDone	;check for rollover
		incf	AddressH,F	;if so then increment high address byte

CheckLineDone:	decfsz	Counter,F	;check if all words have been programmed
		goto	CheckAddress	;if not then go program next word

;-----------------------------------------------------------------------------
;Done programming line of file

LineDone:	movlw	'.'		;line has been programmed so
		call	SerialTransmit	;transmit progress indicator back
		goto	GetNewLine	;go get next line hex file

;-----------------------------------------------------------------------------
;Done programming file so send success indicator and trap execution until reset

FileDone:	movlw	'S'		;programming complete so
		call	SerialTransmit	;transmit success indicator back

		call	LoadStatusAddr	;load address of CodeStatus word
		clrf	EEDATH		;load data to indicate program exists
		clrf	EEDATA		;load data to indicate program exists
		call	FlashWrite
		clrf	PCLATH
		clrf	PCL			; jump to reset vector

;-----------------------------------------------------------------------------
;Error in hex file so send failure indicator and trap error

ErrorMessage:	movlw	'F'		;error occurred so
		call	SerialTransmit	;transmit failure indicator back
TrapError3:	goto	TrapError3	;trap error and wait for reset

;-----------------------------------------------------------------------------
;Load address of CodeStatus word into flash memory address registers
;This routine returns in bank2

LoadStatusAddr:	Bank2			;change from bank0 to bank2
		movlw	high CodeStatus	;load high addr of CodeStatus location
		movwf	EEADRH
		movlw	low CodeStatus	;load low addr of CodeStatus location
		movwf	EEADR
		return

;-----------------------------------------------------------------------------
;Receive two ascii digits and convert into one hex byte
;This routine returns in bank0

GetHexByte:	call	SerialReceive	;get new byte from serial port
		addlw	0xbf		;add -'A' to Ascii high byte
		btfss	STATUS,C	;check if positive
		addlw	0x07		;if not, add 17 ('0' to '9')
		addlw	0x0a		;else add 10 ('A' to 'F') 
		movwf	HexByte		;save nibble
		swapf	HexByte,F	;move nibble to high position

		call	SerialReceive	;get new byte from serial port
		addlw	0xbf		;add -'A' to Ascii low byte
		btfss	STATUS,C	;check if positive
		addlw	0x07		;if not, add 17 ('0' to '9')
		addlw	0x0a		;else add 10 ('A' to 'F') 
		iorwf	HexByte,F	;add low nibble to high nibble
		movf	HexByte,W	;put result in W reg
		addwf	Checksum,F	;add to cumulative checksum
		return

;-----------------------------------------------------------------------------
;Set up USART for asynchronous comms
;Routine is only called once and can be placed in-line saving a call and return
;This routine returns in bank0

SerialSetup:	Bank0			;change from bank3 to bank0
		Bank1			;change from bank0 to bank1
		movlw	BAUD_CONSTANT	;set baud rate 19200 for 4Mhz clock
		movwf	SPBRG
		bsf	TXSTA,BRGH	;baud rate high speed option
		bsf	TXSTA,TXEN	;enable transmission
		Bank0			;change from bank1 to bank0
		bsf	RCSTA,CREN	;enable reception
		bsf	RCSTA,SPEN	;enable serial port
		return

;-----------------------------------------------------------------------------
;Wait for byte to be received in USART and return with byte in W
;This routine returns in bank0

SerialReceive:	Bank0			;change from unknown bank to bank0
		btfss	PIR1,RCIF	;check if data received
		goto	$-1		;wait until new data
		movf	RCREG,W		;get received data into W
		return

;-----------------------------------------------------------------------------
;Transmit byte in W register from USART
;This routine returns in bank0

SerialTransmit:	Bank0			;change from unknown bank to bank0
		btfss	PIR1,TXIF	;check that buffer is empty
		goto	$-1
		movwf	TXREG		;transmit byte
		return

;-----------------------------------------------------------------------------
;Write to a location in the flash program memory
;Address in EEADRH and EEADR, data in EEDATH and EEDATA
;This routine returns in bank3

FlashWrite:	Bank3			;change from bank2 to bank3
		movlw	0x84		;enable writes to program flash
		movwf	EECON1

		movlw	0x55		;do timed access writes
		movwf	EECON2
		movlw	0xaa
		movwf	EECON2
		bsf	EECON1,WR	;begin writing to flash

		nop			;processor halts here while writing
		nop
		return

;-----------------------------------------------------------------------------
;Read from a location in the flash program memory
;Address is in EEADRH and EEADR, data returned in EEDATH and EEDATA
;Routine is only called once and can be placed in-line saving a call and return
;This routine returns in bank3 and is called when in bank2

FlashRead:	movlw	0x1f		;keep address within range
		andwf	EEADRH,F

		Bank3			;change from bank2 to bank3
		movlw	0x80		;enable reads from program flash
		movwf	EECON1

		bsf	EECON1,RD	;read from flash

		nop			;processor waits while reading
		nop
		return

;-----------------------------------------------------------------------------



        end
