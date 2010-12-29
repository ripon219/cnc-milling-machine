; CONVERTED TASM PIC16F877 PICGN220.ASM TO MPASM PICGN220_M.ASM 11-27-2002 10:46:10

                list p=PIC16F877,r=dec
                                        ;PICGN220.ASM 21APR00 - JOHN BECKER - EPE PIC FUNCTION GEN + FREQ COUNT
                 
                                        ;PIC16F877-4, 3.2768Hz, WDT OFF, POR ON, XTAL XT
		__CONFIG H'3F31'                 
                                        ;Config register bits (all PIC TOOLKIT MK2 defaults)
                                        ; CP1 CP0 DBG NIL WRT CPD LVP BOR CP1 CP0 POR WDT OS1 OS0
                                        ;  1   1   1   1   1   1   0   0   1   1   0   0   0   1
                                        ;N.B. Logic 1/0 do NOT necessarily mean that the function is On/Off
                                        ;respectively - refer to PIC '87 data sheet.
                 
#DEFINE         PAGE0 BCF STATUS,RP0
#DEFINE         PAGE1 BSF STATUS,RP0
                 
INDF            EQU H'00'               ;page 0, 1, 2, 3
TMR0            EQU H'01'               ;page 0, 2
OPSHUN          EQU H'01'               ;page 1, 3
PCL             EQU H'02'               ;page 0, 1, 2, 3
STATUS          EQU H'03'               ;page 0, 1, 2, 3
FSR             EQU H'04'               ;page 0, 1, 2, 3
                 
PORTA           EQU H'05'               ;page 0
TRISA           EQU H'05'               ;page 1
PORTB           EQU H'06'               ;page 0, 2
TRISB           EQU H'06'               ;page 1, 3
PORTC           EQU H'07'               ;page 0
TRISC           EQU H'07'               ;page 1
PORTD           EQU H'08'               ;page 0
TRISD           EQU H'08'               ;page 1
PORTE           EQU H'09'               ;page 0
TRISE           EQU H'09'               ;page 1
                 
INTCON          EQU H'0B'               ;page 0, 1, 2, 3
ADCON0          EQU H'1F'               ;page 0
ADCON1          EQU H'1F'               ;page 1
                 
LOOPA           EQU H'20'               ;loop used by LCD send routine
LOOPB           EQU H'21'               ;general loop
STORE           EQU H'22'               ;general store
STORE1          EQU H'23'               ;general store
STORE2          EQU H'24'               ;general store
STORE3          EQU H'25'               ;general store
RSLINE          EQU H'26'               ;LCD function flag store
SWITCH          EQU H'27'               ;switch status store
MARK1           EQU H'28'               ;freq count polarity marker 1
MARK2           EQU H'29'               ;freq count polarity marker 2
                 
DEC1            EQU H'2A'               ;decimalisation byte 1
DEC2            EQU H'2B'               ; byte 2
DEC3            EQU H'2C'               ; byte 3
DEC4            EQU H'2D'               ; byte 4
DEC5            EQU H'2E'               ; byte 5
DEC6            EQU H'2F'               ; byte 6 (dummy)      
                 
ANSA1           EQU H'30'               ;decimalisation answer store 1
ANSA2           EQU H'31'               ; answer 2
ANSA3           EQU H'32'               ; answer 3
ANSA4           EQU H'33'               ; answer 4
ANSA5           EQU H'34'               ; answer 5
                 
FREQ0           EQU H'35'               ;frequency counter lsb
FREQ1           EQU H'36'               ;frequency counter nsb
FREQ2           EQU H'37'               ;frequency counter nsb
FREQ3           EQU H'38'               ;frequency counter nsb
FREQ4           EQU H'39'               ;frequency counter msb
CLKCNT          EQU H'3A'               ;timing counter
SLOWIT          EQU H'3B'               ;delay factor for PAUSE
LOOPX           EQU H'3C'               ;delay loop for LCDOUT
                 
OUT7            EQU H'3D'               ;8 bytes for freq store and output to LCD
OUT6            EQU H'3E'
OUT5            EQU H'3F'
OUT4            EQU H'40'
OUT3            EQU H'41'
OUT2            EQU H'42'
OUT1            EQU H'43'
OUT0            EQU H'44'
LCDSTOR1        EQU H'45'
LCDSTOR2        EQU H'46'
STOREC          EQU H'47'
STORED          EQU H'48'
TENFLG          EQU H'49'
TENCNT          EQU H'4A'
RANGE           EQU H'4B'               ;frequency cap selection
SHAPE           EQU H'4C'               ;waveform shape selection
SAMPLE          EQU H'4D'               ;1sec/10sec flag
CLKVAL1         EQU H'4E'
CLKVAL2         EQU H'4F'
                 
                                        ;extends to $7F (max limit)
                 
W               EQU 0
F               EQU 1
C               EQU 0
DC              EQU 1
Z               EQU 2
                 
RP0             EQU 5                   ;STATUS reg
RP1             EQU 6                   ;STATUS reg
GIE             EQU 7                   ;INTCON reg

		ORG H'0000'		; Reset vector
		goto RESET
                 
                 
                ORG H'0004'             ;Interrupt vector address
                goto START              ;Jump to interrupt routine on interrupt
                ORG H'0005'             ;Start of program memory

                 
RESET           clrf INTCON
                 
GIEOFF          bcf INTCON,GIE          ;turn off global interrupts
                btfsc INTCON,GIE
                goto GIEOFF
                 
                goto START
                 
TABLCD          addwf PCL,F             ;LCD initialisation table
                retlw B'00110011'       ;initialise lcd - first byte
                retlw B'00110011'       ;2nd byte (repeat of first)
                retlw B'00110010'       ;set for 4-bit operation
;;;;                retlw B'00100100'       ;set for 1 line
                retlw B'00101100'       ;set for 2 lines
                retlw B'00000110'       ;set entry mode to increment each address
                retlw B'00001100'       ;set display on, cursor off, blink off
                retlw B'00000001'       ;clear display
                retlw B'00000010'       ;return home, cursor & RAM to zero
                                        ;end initialisation table
                 
TBDEC1                                  ;table for decimalisation lsb
                addwf PCL,F             ;add program counter
                retlw H'10'             ;lsb of 10000
                retlw H'E8'             ;lsb of 1000
                retlw H'64'             ;lsb of 100
                retlw H'0A'             ;lsb of 10
                 
TBDEC2                                  ;table for decimalisation msb
                addwf PCL,F             ;add program counter
                retlw H'27'             ;msb of 10000
                retlw H'03'             ;msb of 1000
                retlw 0                 ;msb of 100
                retlw 0                 ;msb of 10
                 
RANGIT          addwf PCL,F             ;range control vals
                retlw B'11111111'
                retlw B'11111101'
                retlw B'11111011'
                retlw B'11110111'
                retlw B'11101111'
                retlw B'11011111'
                retlw B'10111111'
                retlw B'01111111'

WAVEFORM	addwf PCL,F             ;Constant "WaveForm:"
                retlw 'W'
                retlw 'A'
                retlw 'V'
                retlw 'E'
                retlw 'F'
                retlw 'O'
                retlw 'R'
                retlw 'M'
                retlw ':'
                retlw ' '

                 
WAVEIT          addwf PCL,F             ;waveform message routing
                goto SQUARE
                goto TRNGLE
                goto SINE
                 
SINE            movf LOOPA,W
                addwf PCL,F             ;sine message route
                retlw 'S'
                retlw 'I'
                retlw 'N'
                retlw 'E'
                retlw ' '
                retlw ' '
                retlw ' '
                retlw ' '
                 
SQUARE          movf LOOPA,W
                addwf PCL,F             ;square message route
                retlw 'S'
                retlw 'Q'
                retlw 'U'
                retlw 'A'
                retlw 'R'
                retlw 'E'
                retlw ' '
                retlw ' '
                 
TRNGLE          movf LOOPA,W
                addwf PCL,F             ;triangle message route
                retlw 'T'
                retlw 'R'
                retlw 'I'
                retlw 'A'
                retlw 'N'
                retlw 'G'
                retlw 'L'
                retlw 'E'
                 
                                        ;..............
                 
START           bcf STATUS,RP0
                bcf STATUS,RP1
                clrf PORTA
                clrf PORTB
                clrf PORTC
                clrf PORTD
                clrf PORTE
                PAGE1 
                clrf TRISA              ;PORTA as output
                movlw B'00111111'
                movwf TRISB             ;PORTB as input
                movlw 255
                movwf TRISC             ;PORTC as input
                movlw B'11001111'
                movwf TRISD             ;RD0-RD3, RD6-RD7 as input, RD4-RD5 as output
                movlw B'00000111'
                movwf TRISE             ;PORTE as input
                movlw B'00000111'       ;set LHS justify, RA0-RA3 as digital
                movwf ADCON1
                movlw B'10000110'       ;PORTB pullups off, timer 1:128 (1/25th sec)
                movwf OPSHUN
                PAGE0 
                 
                call PAUSIT
LCDSET          clrf LOOPB
                clrf RSLINE
LCDST2          movf LOOPB,W
                call TABLCD
                call LCDOUT
                incf LOOPB,F
                btfss LOOPB,3
                goto LCDST2
                call PAUSIT
                 
                clrf MARK2
                clrf SWITCH
                clrf FREQ0
                clrf FREQ1
                clrf FREQ2
                clrf FREQ3
                clrf FREQ4
                clrf ANSA1
                clrf ANSA2
                clrf ANSA3
                clrf ANSA4
                clrf ANSA5
                clrf DEC1
                clrf DEC2
                clrf DEC3
                clrf DEC4
                clrf DEC5
                clrf DEC6
                clrf TENFLG
                clrf TENCNT
                clrf MARK1
                clrf MARK2
                clrf RANGE
                clrf SAMPLE
                movlw 0                 ;......2
                movwf SHAPE
                call WAVE1
                movlw 25
                movwf CLKVAL1
                movlw 250
                movwf CLKVAL2
                 
                movf CLKVAL1,W
                movwf CLKCNT
                 
                bsf PORTD,4             ;reset counter IC6
                bcf PORTD,4
                bsf PORTD,5             ;enable clock input
                 
                movlw 4
                movwf RANGE
                bcf INTCON,2
                 
                goto SHWRNG
                 
                                        ;............................ END OF SETUP
                 
MAIN            btfss INTCON,2
                goto MF1
                bcf INTCON,2
                 
                movf PORTE,W            ;is a range switch pressed?
                btfss STATUS,Z
                goto RANGESW            ;yes
                btfsc PORTB,0           ;is waveshape switch pressed?
                call WAVEFRM            ;yes
                decfsz CLKCNT,F         ;dec timing counter, is it 0
                goto MF1                ;no
                goto GETFREQ            ;yes
                 
MF1             call INCCNT
                goto MAIN
                 
                 
INCCNT          btfss PORTC,0
                goto MF2
                bsf MARK2,0
                return 
                 
MF2             btfss MARK2,0           ;is MARK2 = 1?
                return                  ;no
                bcf MARK2,0
                incfsz FREQ2,F          ;yes, inc counters accordingly
                return                  ;(responds to negative-going aspect of IC6 Q12)
                incfsz FREQ3,F
                return 
                incf FREQ4,F
                return 
                 
                                        ;............show freq routine
                 
GETFREQ         bcf PORTD,5             ;turn off clock input
                nop 
                nop 
                call INCCNT             ;*****
                movf PORTD,W            ;get pins which include Q0-Q3
                movwf STORED
                movf PORTC,W
                movwf STOREC
                bsf PORTD,5             ;turn on clock input
                 
                bcf STATUS,C            ;shift freq counters right by 4 places
                rrf FREQ4,F
                rrf FREQ3,F
                rrf FREQ2,F
                rrf FREQ1,F
                bcf STATUS,C
                rrf FREQ4,F
                rrf FREQ3,F
                rrf FREQ2,F
                rrf FREQ1,F
                bcf STATUS,C
                rrf FREQ4,F
                rrf FREQ3,F
                rrf FREQ2,F
                rrf FREQ1,F
                bcf STATUS,C
                rrf FREQ4,F
                rrf FREQ3,F
                rrf FREQ2,F
                rrf FREQ1,F
                 
                btfsc STORED,1          ;Q1
                bsf FREQ0,0
                btfsc STORED,2          ;Q2
                bsf FREQ0,1
                btfsc STORED,3          ;Q3
                bsf FREQ0,2
                btfsc STOREC,4          ;Q4
                bsf FREQ0,3
                btfsc STOREC,6          ;Q5
                bsf FREQ0,4
                btfsc STOREC,7          ;Q6
                bsf FREQ0,5
                btfsc STOREC,5          ;Q7
                bsf FREQ0,6
                btfsc STOREC,3          ;Q8
                bsf FREQ0,7
                 
                btfsc STORED,0          ;Q9
                bsf FREQ1,0
                btfsc STOREC,2          ;Q10
                bsf FREQ1,1
                btfsc STOREC,1          ;Q11
                bsf FREQ1,2
                btfsc STOREC,0          ;Q12
                bsf FREQ1,3
                 
                movlw FREQ0
                call DECIML             ;decimalise digits 1 to 4 
                movf DEC1,W             ;copy answer into OUT-OUT3
                movwf OUT0
                movf ANSA1,W
                movwf OUT1
                movf ANSA2,W
                movwf OUT2
                movf ANSA3,W
                movwf OUT3
                 
GF3             movf ANSA4,W            ;copy overflow val into prev freq positions
                movwf FREQ0
                movf ANSA5,W
                movwf FREQ1
                clrf FREQ2
                clrf FREQ3
                clrf FREQ4
                movlw FREQ0
                call DECIML             ;now decimal digits 5 to 8
                 
                movf ANSA3,W            ;copy answer into OUT4 to OUT7
                movwf OUT7
                movf ANSA2,W
                movwf OUT6
                movf ANSA1,W
                movwf OUT5
                movf DEC1,W
                movwf OUT4

; Added code to print "Freq:"

                 
GF4             call PFREQ               ;output freq data to LCD
                bsf RSLINE,4
                clrf MARK1              ;clear leading zero flag
                 
                movlw OUT7
                movwf FSR
                movlw 8
                movwf LOOPA
MAINA           movf INDF,W
                btfss STATUS,Z          ;is val = 0?
                goto MAINB              ;no
                btfsc MARK1,0           ;has val >0 been output?
                goto MAINB              ;yes
                movf LOOPA,W            ;is it the last byte of the loop?
                xorlw 1
                btfsc STATUS,Z
                goto MAINB              ;yes
                movlw ' '               ;clear leading zero
                goto MAINC
                 
MAINB           bsf MARK1,0             ;format as ASCII decimal val
                andlw 15
                iorlw 48
                 
MAINC           call LCDOUT
                incf FSR,F
                decfsz LOOPA,F
                goto MAINA
                btfss SAMPLE,0
                goto GF5
                call LCD12
                bsf RSLINE,4
                movlw '.'
                call LCDOUT
                 
                decf FSR,F
                movf INDF,W
                andlw 15
                iorlw 48
                call LCDOUT
                goto GF6
                 
GF5             movlw ' '
                call LCDOUT
                 
GF6             movlw 'H'
                call LCDOUT
                movlw 'z'
                call LCDOUT
                 
MAINX           clrf FREQ0              ;clear freq counters
                clrf FREQ1
                clrf FREQ2
                clrf FREQ3
                clrf FREQ4
                clrf MARK2
                 
                movf CLKVAL1,W
                btfsc SAMPLE,0          ;is 10sec rate on?
                movf CLKVAL2,W
                movwf CLKCNT
                bsf PORTD,4             ;reset counter IC6
                bcf PORTD,4
                bsf PORTD,5             ;enable clock input
                 
                clrf TMR0
                bcf INTCON,2            ;clear interrupt
                 
                goto MAIN               ;start sampling next batch
                 
                                        ;.............
                 
PAUSIT          movlw 7
                movwf SLOWIT
                bcf INTCON,2
PAUSE           btfss INTCON,2
                goto PAUSE
                bcf INTCON,2
                decfsz SLOWIT,F
                goto PAUSE
                return 
                 

; Print "Freq: " on second line
PFREQ
		call LCD5
                bsf RSLINE,4
                movlw 'F'
                call LCDOUT
                movlw 'r'
                call LCDOUT
                movlw 'e'
                call LCDOUT
                movlw 'q'
                call LCDOUT
                movlw ':'
                call LCDOUT
                movlw ' '
                call LCDOUT
		return

                                        ;................
                 
LCD1            movlw B'10000000'       ;WaveForm: 
                goto LCDLIN
LCD5            movlw B'11000000'	; 'COUNTING ' or '  Ok       0'
                goto LCDLIN
LCD12           movlw B'11001101'	; location of decimal point
                goto LCDLIN
                 
LCD21           movlw B'10010100'	; 'Range'
                goto LCDLIN
LCD27           movlw B'11010100'	; 'Time xx sec'
                goto LCDLIN
                 
                 
LCDLIN          bcf RSLINE,4
                 
LCDOUT          movwf STORE
                movlw 50
                movwf LOOPX
DELAY           decfsz LOOPX,F
                goto DELAY
                call SENDIT
SENDIT          swapf STORE,F
                movf STORE,W
                andlw 15
                iorwf RSLINE,W
                movwf LCDSTOR1
                clrf LCDSTOR2
                movlw 6
                movwf LOOPX
                 
SHIFT           bcf STATUS,C            ;rearrange bits to suit PORTA pin order
                rrf LCDSTOR1,F
                rlf LCDSTOR2,F
                decfsz LOOPX,F
                goto SHIFT
                movf LCDSTOR2,W
                movwf PORTA
                bsf PORTA,0             ;toggle LCD line E
                bcf PORTA,0
                bsf PORTA,5             ;turn on IC2 sync output line
                return 
                 
                                        ;..............convert binary to hex - not used in final model
                 
LCDHEX          movwf STORE2            ;split & format decimal byte as HEX for LCD
                swapf STORE2,W          ;get tens nibble
                andlw 15
                movwf STORE1
                addlw 6
                btfss STATUS,DC
                goto HEX2
                movf STORE1,W
                addlw 55                ;set as alpha
                goto HEX3
HEX2            movf STORE1,W
                iorlw 48                ;set as numeral
HEX3            call LCDOUT             ;send it
                movf STORE2,W           ;get units
                andlw 15
                movwf STORE1
                addlw 6
                btfss STATUS,DC
                goto HEX4
                movf STORE1,W
                addlw 55                ;set as alpha
                goto HEX5
HEX4            movf STORE1,W
                iorlw 48                ;set as numeral
HEX5            call LCDOUT
                return 
                 
                                        ;........... used to convert binary bytes to decimal for LCD
                 
DECIML                                  ;decimalise binary number
                                        ;copy source into working area at DEC
                                        ;source address is brought in on W
                                        ;DEC1-5 becomes source
                                        ;answer goes into ANSA
                call COPYFD             ;copy data at FSR into DEC
                clrf STORE1
                clrf STORE3
                clrf ANSA5
                movlw ANSA4             ;set answer store address
                movwf FSR
                clrf LOOPA
                 
DCML0           movf LOOPA,W
                call TBDEC1             ;subtract lsb from source value
                subwf DEC1,F
                btfsc STATUS,C          ;is there a borrow?
                goto DCML1              ;no, so sub msb
                movlw 1
                subwf DEC2,F            ;yes so decrement next byte
                btfsc STATUS,C          ;is there a borrow?
                goto DCML1              ;no, so sub msb
                movlw 1                 ;yes
                subwf DEC3,F
                btfsc STATUS,C          ;is there a borrow?
                goto DCML1              ;no, so sub msb
                movlw 1                 ;yes
                subwf DEC4,F
                btfsc STATUS,C          ;is there a borrow?
                goto DCML1              ;no, so sub msb
                movlw 1                 ;yes
                subwf DEC5,F
                btfsc STATUS,C          ;is there a borrow?
                goto DCML1              ;no, so sub msb
                 
DCML3           movf LOOPA,W            ;yes, so re-add last table values lsb
                call TBDEC1
                addwf DEC1,F
                btfss STATUS,C          ;is there a carry?
                goto DCML5              ;no so exit loop
                incf DEC2,F             ;yes so inc next byte
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML5              ;no so exit loop
                incf DEC3,F             ;yes
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML5              ;no so exit loop
                incf DEC4,F             ;yes
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML5              ;no so exit loop
                incf DEC5,F             ;yes
                goto DCML5              ;exit loop
                 
DCML1           movf LOOPA,W
                call TBDEC2             ;subtract msb from source value
                subwf DEC2,F
                btfsc STATUS,C          ;is there a borrow?
                goto DCML2              ;no
                movlw 1
                subwf DEC3,F            ;yes so decrement next byte
                btfsc STATUS,C          ;is there a borrow?
                goto DCML2              ;no
                movlw 1
                subwf DEC4,F            ;yes so decrement next byte
                btfsc STATUS,C          ;is there a borrow?
                goto DCML2              ;no
                movlw 1
                subwf DEC5,F            ;yes so decrement next byte
                btfss STATUS,C          ;is there a borrow?
                goto DCML4              ;yes
                 
DCML2           incfsz STORE1,F         ;inc counter & continue looping till zero
                goto DCML0
                incf STORE3,F
                goto DCML0
                 
DCML4           movf LOOPA,W            ;re-add last table values lsb
                call TBDEC1
                addwf DEC1,F
                btfss STATUS,C          ;is there a carry?
                goto DCML4A             ;no so add msb
                incf DEC2,F             ;yes so inc next byte
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML4A             ;no so add msb
                incf DEC3,F             ;yes, so inc next byte
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML4A             ;no so add msb
                incf DEC4,F             ;yes, so inc next byte
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML4A             ;no so add msb
                incf DEC5,F             ;yes, so inc next byte
                 
DCML4A          movf LOOPA,W            ;re-add last table values msb
                call TBDEC2
                addwf DEC2,F
                btfss STATUS,C          ;is there a carry?
                goto DCML5              ;no
                incf DEC3,F             ;yes, so inc next byte
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML5              ;no
                incf DEC4,F             ;yes, so inc next byte
                btfss STATUS,Z          ;is there a carry? (is it zero)
                goto DCML5              ;no
                incf DEC5,F             ;yes, so inc next byte
                 
DCML5           movf STORE3,W
                btfss STATUS,Z
                movwf ANSA5
                movf STORE1,W           ;store counter and loop for next values
                movwf INDF              ;INDF here is ANSA group
                 
                clrf STORE1
                clrf STORE3
                decf FSR,F
                 
                incf LOOPA,F
                btfss LOOPA,2           ;is it = 4?
                goto DCML0              ;no
                 
DCML6           return 
                 
COPYFD          movwf FSR               ;copy data at FSR to DEC
                movf INDF,W
                movwf DEC1
                incf FSR,F
                movf INDF,W
                movwf DEC2
                incf FSR,F
                movf INDF,W
                movwf DEC3
                incf FSR,F
                movf INDF,W
                movwf DEC4
                incf FSR,F
                movf INDF,W
                movwf DEC5
                clrf DEC6
                return 
                 
                                        ;...........range switch
                 
RANGESW         btfsc PORTE,2
                goto SAMRATE
                call OK
                 
                btfsc PORTE,1
                goto DECRNG
                 
INCRNG          incf RANGE,W
                goto SHWRNG
DECRNG          decf RANGE,W
                 
SHWRNG          andlw 7
                movwf RANGE
                call LCD21
                bsf RSLINE,4
                movlw 'F'
                call LCDOUT
                movlw 'r'
                call LCDOUT
                movlw 'e'
                call LCDOUT
                movlw 'q'
                call LCDOUT
                movlw ' '
                call LCDOUT
                movlw 'R'
                call LCDOUT
                movlw 'a'
                call LCDOUT
                movlw 'n'
                call LCDOUT
                movlw 'g'
                call LCDOUT
                movlw 'e'
                call LCDOUT
                movlw ' '
                call LCDOUT
                 
                movf RANGE,W
                sublw 7
                 
                iorlw 48
                call LCDOUT
                movf PORTB,W
                andlw B'11000000'
                movwf PORTB
                 
                bcf PORTD,6
                bcf PORTD,7
                 
                movf RANGE,W
                call RANGIT
                andlw B'00111111'
                PAGE1 
                movwf TRISB
                PAGE0 
                movf RANGE,W
                call RANGIT
                andlw B'11000000'
                iorlw B'00001111'
                PAGE1 
                movwf TRISD
                PAGE0 
                call PAUSIT
                call PAUSIT
                movf PORTE,W
                andlw 3
                btfss STATUS,Z
                goto RANGESW
                goto SAM1
                 
                                        ;...........set sample rate 1sec/10sec
                 
SAMRATE         incf SAMPLE,F
                bcf SAMPLE,1
                call OK
                 
SAM1            clrf FREQ0              ;clear freq counters
                clrf FREQ1
                clrf FREQ2
                clrf FREQ3
                clrf FREQ4
                clrf MARK2
                 
                call LCD27
                bsf RSLINE,4
                movlw 'C'
                call LCDOUT
                movlw 'o'
                call LCDOUT
                movlw 'u'
                call LCDOUT
                movlw 'n'
                call LCDOUT
                movlw 't'
                call LCDOUT
                movlw ' '
                call LCDOUT
                movlw 'T'
                call LCDOUT
                movlw 'i'
                call LCDOUT
                movlw 'm'
                call LCDOUT
                movlw 'e'
                call LCDOUT
                movlw ':'
                call LCDOUT
                movlw ' '
                call LCDOUT
                movlw '1'
                call LCDOUT
                movlw ' '
                btfsc SAMPLE,0
                movlw '0'
                call LCDOUT
                movlw 's'
                call LCDOUT
                movlw 'e'
                call LCDOUT
                movlw 'c'
                call LCDOUT
                 
SAM3            movf PORTE,W
                andlw 7
                btfss STATUS,Z
                goto SAM3
                 
                call PFREQ               ;output freq data to LCD
                bsf RSLINE,4
                movlw ' '
                call LCDOUT
                movlw ' '
                call LCDOUT
                movlw 'C'
                call LCDOUT
                movlw 'O'
                call LCDOUT
                movlw 'U'
                call LCDOUT
                movlw 'N'
                call LCDOUT
                movlw 'T'
                call LCDOUT
                movlw 'I'
                call LCDOUT
                movlw 'N'
                call LCDOUT
                movlw 'G'
                call LCDOUT
                movlw ' '
                call LCDOUT
                 
                movf CLKVAL1,W
                btfsc SAMPLE,0
                movf CLKVAL2,W
                movwf CLKCNT
                 
                bsf PORTD,4             ;reset counter IC6
                bcf PORTD,4
                 
                goto MAIN
                 
OK              call PFREQ               ;output freq data to LCD
                bsf RSLINE,4
                movlw ' '
                call LCDOUT
                movlw ' '
                call LCDOUT
                movlw 'O'
                call LCDOUT
                movlw 'K'
                call LCDOUT
                 
                movlw 6
                movwf LOOPA
OK2             movlw ' '
                call LCDOUT
                decfsz LOOPA,F
                goto OK2
                movlw '0'
                call LCDOUT
                return 
                 
                                        ;........
                 
WAVEFRM         incf SHAPE,F
                movf SHAPE,W
                xorlw B'00000011'
                btfsc STATUS,Z
                clrf SHAPE
                 
WAVE1           call LCD1               ;output freq data to LCD
                bsf RSLINE,4

		clrf LOOPB
WF2 		movf LOOPB,W
                call WAVEFORM
                call LCDOUT
                incf LOOPB,F
                btfss LOOPB,3		; check the eight bit
                goto WF2
		btfss LOOPB,1		;  and the 2 bit makes 10
		goto WF2

                clrf LOOPA
WAVE2           movf SHAPE,W
                call WAVEIT
                call LCDOUT
                incf LOOPA,F
                btfss LOOPA,3		; checks for eight positions
                goto WAVE2
                 
                swapf SHAPE,W
                movwf STORE
                rlf STORE,F
                rlf STORE,W
                andlw B'11000000'
                movwf STORE
                movf PORTB,W
                andlw B'00111111'
                iorwf STORE,W
                movwf PORTB
                 
WAVE3           btfsc PORTB,0
                goto WAVE3
                call PAUSIT
                return 
                 
                end 
                 
