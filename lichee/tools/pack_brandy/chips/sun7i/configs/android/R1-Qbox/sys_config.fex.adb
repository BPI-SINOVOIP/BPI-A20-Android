;A20 PAD application
;-------------------------------------------------------------------------------
; 说明：
;   1. 脚本中的字符串区分大小写，用户可以修改"="后面的数值，但是不要修改前面的字符串     
;   2. 新增主键和子键的名称必须控制在32个字符以内，不包括32个
;   3. 所以的注释以“;”开始，单独占据一行
;   4. 注释不可和配置项同行，例如：主键和子健后面不能添加任何形式的注释
;
; gpio的描述形式：Port:端口+组内序号<功能分配><内部电阻状态><驱动能力><输出电平状态>
;           例如：port:PA0<0><default><default><default>
;-------------------------------------------------------------------------------
[product]
version             = "100"
machine             = "R1-Qbox"

[platform]
eraseflag           = 1

[target]
boot_clock          = 912
dcdc2_vol           = 1400
dcdc3_vol           = 1250
ldo2_vol            = 3000
ldo3_vol            = 2800
ldo4_vol            = 2800
power_start         = 3
storage_type        = -1
usb_recovery        = 1
lradc_used          = 0


[axp15_para]
dcdc1_vol 	= 3000
dcdc2_vol 	= 1250
dcdc3_vol 	= 1500
dcdc4_vol 	= 1250
aldo1_vol	= 3000
aldo2_vol	= 3000
dldo1_vol	= 3300
dldo2_vol	= 3300

[clock]
pll3                = 297
pll4                = 300
pll6                = 600
pll7                = 297
pll8                = 336

;-------------------------------------------------------------------------------
; 	boot阶段上电初始化GPIO
;		use		:模块使能端     置1：开启模块   置0：关闭模块
;		gpiox ：上电初始化gpio （名称自定，但不能重复，并且GPIO允许可以多个）
;-------------------------------------------------------------------------------
[boot_init_gpio]
;use		    = 0
lte_pwr		    = port:PH24<1><default><default><1>


[card_boot]
logical_start       = 40960
sprite_gpio0        = port:PH20<1><default><default><0>
sprite_work_delay        = 500
sprite_err_delay         = 200

[card0_boot_para]
card_ctrl           = 0
card_high_speed     = 1
card_line           = 4
sdc_d1              = port:PF0<2><1><default><default>
sdc_d0              = port:PF1<2><1><default><default>
sdc_clk             = port:PF2<2><1><default><default>
sdc_cmd             = port:PF3<2><1><default><default>
sdc_d3              = port:PF4<2><1><default><default>
sdc_d2              = port:PF5<2><1><default><default>

[card2_boot_para]
card_ctrl           = 2
card_high_speed     = 1
card_line           = 4
sdc_cmd             = port:PC6<3><1>
sdc_clk             = port:PC7<3><1>
sdc_d0              = port:PC8<3><1>
sdc_d1              = port:PC9<3><1>
sdc_d2              = port:PC10<3><1>
sdc_d3              = port:PC11<3><1>

[twi_para]
twi_port            = 0
twi_scl             = port:PB0<2><default><default><default>
twi_sda             = port:PB1<2><default><default><default>

[uart_para]
uart_debug_port     = 0
uart_debug_tx       = port:PB22<2><1><default><default>
uart_debug_rx       = port:PB23<2><1><default><default>

[uart_force_debug]
uart_debug_port          = 0
uart_debug_tx            =port:PF2<4><1><default><default>
uart_debug_rx            =port:PF4<4><1><default><default>

[jtag_para]
jtag_enable         = 1
jtag_ms             = port:PB14<3><default><default><default>
jtag_ck             = port:PB15<3><default><default><default>
jtag_do             = port:PB16<3><default><default><default>
jtag_di             = port:PB17<3><default><default><default>

;---------------------------------------------------------------------------------------------------------
; if 1 == standby_mode, then support super standby;
; else, support normal standby.
;---------------------------------------------------------------------------------------------------------
[pm_para]
standby_mode		= 0
usbhid_wakeup_enable = 0

;-------------------------------------------------------------------------------
;sdram configuration
;-------------------------------------------------------------------------------
[dram_para]
dram_baseaddr       = 0x40000000
dram_clk            = 432
dram_type           = 3
dram_rank_num       = 0xffffffff
dram_chip_density   = 0xffffffff
dram_io_width       = 0xffffffff
dram_bus_width      = 0xffffffff
dram_cas            = 9
dram_zq             = 0x7f
dram_odt_en         = 0
dram_size           = 0xffffffff
dram_tpr0           = 0x38D48893
dram_tpr1           = 0xa0a0
dram_tpr2           = 0x22a00
dram_tpr3           = 0x0
dram_tpr4           = 0x0
dram_tpr5           = 0x0
dram_emr1           = 0x4
dram_emr2           = 0x10
dram_emr3           = 0x0

;-------------------------------------------------------------------------------
;Mali configuration
;-------------------------------------------------------------------------------
[mali_para]
mali_used           = 1
mali_clkdiv         = 1

;-------------------------------------------------------------------------------
;Ethernet MAC configuration
;-------------------------------------------------------------------------------
[emac_para]
emac_used           = 0
emac_rxd3           = port:PA00<2><default><default><default>
emac_rxd2           = port:PA01<2><default><default><default>
emac_rxd1           = port:PA02<2><default><default><default>
emac_rxd0           = port:PA03<2><default><default><default>
emac_txd3           = port:PA04<2><default><default><default>
emac_txd2           = port:PA05<2><default><default><default>
emac_txd1           = port:PA06<2><default><default><default>
emac_txd0           = port:PA07<2><default><default><default>
emac_rxclk          = port:PA08<2><default><default><default>
emac_rxerr          = port:PA09<2><default><default><default>
emac_rxdV           = port:PA10<2><default><default><default>
emac_mdc            = port:PA11<2><default><default><default>
emac_mdio           = port:PA12<2><default><default><default>
emac_txen           = port:PA13<2><default><default><default>
emac_txclk          = port:PA14<2><default><default><default>
emac_crs            = port:PA15<2><default><default><default>
emac_col            = port:PA16<2><default><default><default>
emac_reset          = port:PA17<1><default><default><default>
emac_power          = port:PH15<1><default><default><0>
[gmac_para]
gmac_used = 1
gmac_rxd3 = port:PA00<5><default><3><default>
gmac_rxd2 = port:PA01<5><default><3><default>
gmac_rxd1 = port:PA02<5><default><3><default>
gmac_rxd0 = port:PA03<5><default><3><default>
gmac_txd3 = port:PA04<5><default><3><default>
gmac_txd2 = port:PA05<5><default><3><default>
gmac_txd1 = port:PA06<5><default><3><default>
gmac_txd0 = port:PA07<5><default><3><default>
gmac_rxclk = port:PA08<5><default><3><default>
gmac_rxerr = port:PA09<0><default><3><default>
gmac_rxctl = port:PA10<5><default><3><default>
gmac_mdc = port:PA11<5><default><3><default>
gmac_mdio = port:PA12<5><default><3><default>
gmac_txctl = port:PA13<5><default><3><default>
gmac_txclk = port:PA14<0><default><3><default>
gmac_txck = port:PA15<5><default><3><default>
gmac_clkin = port:PA16<5><default><3><default>
gmac_txerr = port:PA17<0><default><3><default>
[gmac_phy_power]
gmac_phy_power_en = port:PH23<1><default><default><0>

;-------------------------------------------------------------------------------
;phy configuration
;-------------------------------------------------------------------------------
[phy_para]
b53_used           = 1

;-------------------------------------------------------------------------------
;i2c configuration
;-------------------------------------------------------------------------------
[twi0_para]
twi0_used           = 1
twi0_scl            = port:PB0<2><default><default><default>
twi0_sda            = port:PB1<2><default><default><default>

[twi1_para]
twi1_used           = 1
twi1_scl            = port:PB18<2><default><default><default>
twi1_sda            = port:PB19<2><default><default><default>

[twi2_para]
twi2_used           = 1
twi2_scl            = port:PB20<2><default><default><default>
twi2_sda            = port:PB21<2><default><default><default>

[twi3_para]
twi3_used           = 1
twi3_scl            = port:PI0<3><default><default><default>
twi3_sda            = port:PI1<3><default><default><default>

[twi4_para]
twi4_used           = 0
twi4_scl            = port:PI2<3><default><default><default>
twi4_sda            = port:PI3<3><default><default><default>

;-------------------------------------------------------------------------------
;uart configuration
;uart_type ---  2 (2 wire), 4 (4 wire), 8 (8 wire, full function)
;-------------------------------------------------------------------------------
[uart_para0]
uart_used           = 1
uart_port           = 0
uart_type           = 2
uart_tx             = port:PB22<2><1><default><default>
uart_rx             = port:PB23<2><1><default><default>

[uart_para1]
uart_used           = 0
uart_port           = 1
uart_type           = 8
uart_tx             = port:PA10<4><1><default><default>
uart_rx             = port:PA11<4><1><default><default>
uart_rts            = port:PA12<4><1><default><default>
uart_cts            = port:PA13<4><1><default><default>
uart_dtr            = port:PA14<4><1><default><default>
uart_dsr            = port:PA15<4><1><default><default>
uart_dcd            = port:PA16<4><1><default><default>
uart_ring           = port:PA17<4><1><default><default>

[uart_para2]
uart_used           = 1
uart_port           = 2
uart_type           = 4
uart_tx             = port:PI18<3><1><default><default>
uart_rx             = port:PI19<3><1><default><default>
uart_rts            = port:PI16<3><1><default><default>
uart_cts            = port:PI17<3><1><default><default>

[uart_para3]
uart_used           = 1
uart_port           = 3
uart_type           = 4
uart_tx             = port:PG06<4><1><default><default>
uart_rx             = port:PG07<4><1><default><default>
uart_rts            = port:PG08<4><1><default><default>
uart_cts            = port:PG09<4><1><default><default>

[uart_para4]
uart_used           = 1
uart_port           = 4
uart_type           = 2
uart_tx             = port:PG10<4><1><default><default>
uart_rx             = port:PG11<4><1><default><default>

[uart_para5]
uart_used           = 0
uart_port           = 5
uart_type           = 2
uart_tx             = port:PH06<4><1><default><default>
uart_rx             = port:PH07<4><1><default><default>

[uart_para6]
uart_used           = 0
uart_port           = 6
uart_type           = 2
uart_tx             = port:PA12<3><1><default><default>
uart_rx             = port:PA13<3><1><default><default>

[uart_para7]
uart_used           = 0
uart_port           = 7
uart_type           = 2
uart_tx             = port:PA14<3><1><default><default>
uart_rx             = port:PA15<3><1><default><default>

;-------------------------------------------------------------------------------
;spi configuration
;bus_num must be set as the same as spi number 
;-------------------------------------------------------------------------------
[spi0_para]
spi_used            = 0
spi_cs_bitmap       = 1
spi_cs0             = port:PI10<2><default><default><default>
spi_cs1             = port:PI14<2><default><default><default>
spi_sclk            = port:PI11<2><default><default><default>
spi_mosi            = port:PI12<2><default><default><default>
spi_miso            = port:PI13<2><default><default><default>

[spi1_para]
spi_used            = 0
spi_cs_bitmap       = 1
spi_cs0             = port:PA00<3><default><default><default>
spi_cs1             = port:PA04<3><default><default><default>
spi_sclk            = port:PA01<3><default><default><default>
spi_mosi            = port:PA02<3><default><default><default>
spi_miso            = port:PA03<3><default><default><default>

[spi2_para]
spi_used            = 0
spi_cs_bitmap       = 1
spi_cs0             = port:PC19<3><default><default><default>
spi_cs1             = port:PB13<2><default><default><default>
spi_sclk            = port:PC20<3><default><default><default>
spi_mosi            = port:PC21<3><default><default><default>
spi_miso            = port:PC22<3><default><default><default>

[spi3_para]
spi_used            = 0
spi_cs_bitmap       = 1
spi_cs0             = port:PA05<3><default><default><default>
spi_cs1             = port:PA09<3><default><default><default>
spi_sclk            = port:PA06<3><default><default><default>
spi_mosi            = port:PA07<3><default><default><default>
spi_miso            = port:PA08<3><default><default><default>

[spi_devices]
spi_dev_num = 1

[spi_board0]
modalias = "spidev"
max_speed_hz = 12000000
bus_num = 0
chip_select = 0
mode = 3
full_duplex = 0
manual_cs = 0

;----------------------------------------------------------------------------------
;resistance tp configuration
;----------------------------------------------------------------------------------
[rtp_para]
rtp_used                   = 1
rtp_screen_size            = 5
rtp_regidity_level         = 5
rtp_press_threshold_enable = 0
rtp_press_threshold        = 0x1f40
rtp_sensitive_level        = 0xf
rtp_exchange_x_y_flag      = 0
;-------------------------------------------------------------------------------
;capacitor tp configuration
;external int function
;wakeup output function
;notice:
;   tp_int_port &  tp_io_port use the same port
;-------------------------------------------------------------------------------
[ctp_para]
ctp_used                = 0
ctp_twi_id              = 3
ctp_twi_name            = "ft5x_ts"
ctp_screen_max_x        = 1024
ctp_screen_max_y        = 600
ctp_revert_x_flag       = 0
ctp_revert_y_flag       = 0
ctp_exchange_x_y_flag   = 0

ctp_int_port            = port:PH09<6><default><default><default>
ctp_wakeup              = port:PH07<1><default><default><1>
ctp_io_port             = port:PH09<0><default><default><default>
;--------------------------------------------------------------------------------
; CTP automatic detection configuration
;ctp_detect_used  --- Whether startup automatic inspection function. 1:used,0:unused
;Module name postposition 1 said detection, 0 means no detection. 
;--------------------------------------------------------------------------------
[ctp_list_para]
ctp_det_used              = 1
ft5x_ts                   = 1
gt82x                     = 0
gslX680                   = 0
gt9xx_ts                  = 0
gt811                     = 0
;-------------------------------------------------------------------------------
;touch key configuration
;-------------------------------------------------------------------------------
[tkey_para]
tkey_used           = 0
tkey_twi_id         = 2
tkey_twi_addr       = 0x62
tkey_int            = port:PI13<6><default><default><default>

;-------------------------------------------------------------------------------
;motor configuration
;-------------------------------------------------------------------------------
[motor_para]
motor_used          = 0
motor_shake         = port:PB03<1><default><default><1>

;-------------------------------------------------------------------------------
;nand flash configuration
;-------------------------------------------------------------------------------
[nand_para]
nand_used           = 0
nand_we             = port:PC00<2><default><default><default>
nand_ale            = port:PC01<2><default><default><default>
nand_cle            = port:PC02<2><default><default><default>
nand_ce1            = port:PC03<2><default><default><default>
nand_ce0            = port:PC04<2><default><default><default>
nand_nre            = port:PC05<2><default><default><default>
nand_rb0            = port:PC06<2><default><default><default>
nand_rb1            = port:PC07<2><default><default><default>
nand_d0             = port:PC08<2><default><default><default>
nand_d1             = port:PC09<2><default><default><default>
nand_d2             = port:PC10<2><default><default><default>
nand_d3             = port:PC11<2><default><default><default>
nand_d4             = port:PC12<2><default><default><default>
nand_d5             = port:PC13<2><default><default><default>
nand_d6             = port:PC14<2><default><default><default>
nand_d7             = port:PC15<2><default><default><default>
nand_wp             = port:PC16<2><default><default><default>
nand_ce2            = port:PC17<2><default><default><default>
nand_ce3            = port:PC18<2><default><default><default>
nand_ce4            =
nand_ce5            =
nand_ce6            =
nand_ce7            =
nand_spi            = port:PC23<3><default><default><default>
nand_ndqs           = port:PC24<2><default><default><default>
good_block_ratio    = 0
id_number_ctl 		= 0x3
nand_p0 			= 0x28010020
nand_p1 			= 0x01eeeeee

;-------------------------------------------------------------------------------
;disp init configuration
;
;disp_mode            (0:screen0<screen0,fb0> 1:screen1<screen1,fb0> 2:two_diff_screen_diff_contents<screen0,screen1,fb0,fb1>
;                      3:two_same_screen_diff_contets<screen0,screen1,fb0> 4:two_diff_screen_same_contents<screen0,screen1,fb0>)
;screenx_output_type  (0:none; 1:lcd; 2:tv; 3:hdmi; 4:vga)
;screenx_output_mode  (used for tv/hdmi output, 0:480i 1:576i 2:480p 3:576p 4:720p50 5:720p60 6:1080i50 7:1080i60 8:1080p24 9:1080p50 10:1080p60 11:pal 14:ntsc)
;screenx_output_mode  (used for vga output, 0:1680*1050 1:1440*900 2:1360*768 3:1280*1024 4:1024*768 5:800*600 6:640*480 10:1920*1080 11:1280*720)
;fbx format           (4:RGB655 5:RGB565 6:RGB556 7:ARGB1555 8:RGBA5551 9:RGB888 10:ARGB8888 12:ARGB4444)
;fbx pixel sequence   (0:ARGB 1:BGRA 2:ABGR 3:RGBA) --- 0 for linux, 2 for android
;lcd0_bright          (lcd0 init bright,the range:[0,256],default:197
;lcd1_bright          (lcd1 init bright,the range:[0,256],default:197
;-------------------------------------------------------------------------------
[disp_init]
disp_init_enable        = 1
disp_mode               = 4

screen0_output_type     = 3
screen0_output_mode     = 5

screen1_output_type     = 1
screen1_output_mode     = 4

fb0_framebuffer_num     = 2
fb0_format              = 10
fb0_pixel_sequence      = 0
fb0_scaler_mode_enable  = 1
fb0_width               = 0
fb0_height              = 0

fb1_framebuffer_num     = 2
fb1_format              = 10
fb1_pixel_sequence      = 0
fb1_scaler_mode_enable  = 0
fb1_width               = 0
fb1_height              = 0

lcd0_backlight          = 197
lcd1_backlight          = 197

lcd0_bright             = 50
lcd0_contrast           = 50
lcd0_saturation         = 57
lcd0_hue                = 50

lcd1_bright             = 50
lcd1_contrast           = 50
lcd1_saturation         = 57
lcd1_hue                = 50

;-------------------------------------------------------------------------------
;lcd0 configuration

;lcd_dclk_freq:      in MHZ unit
;lcd_pwm_freq:       in HZ unit
;lcd_if:             0:hv(sync+de); 1:8080; 2:ttl; 3:lvds; 4:hv2dsi
;lcd_width:          width of lcd in mm
;lcd_height:         height of lcd in mm
;lcd_hbp:            hsync back porch
;lcd_ht:             hsync total cycle
;lcd_vbp:            vsync back porch
;lcd_vt:             vysnc total cycle *2
;lcd_hv_if:          0:hv parallel 1:hv serial
;lcd_hv_smode:       0:RGB888 1:CCIR656
;lcd_hv_s888_if      serial RGB format
;lcd_hv_syuv_if:     serial YUV format
;lcd_hspw:           hsync plus width
;lcd_vspw:           vysnc plus width
;lcd_lvds_ch:        0:single channel; 1:dual channel
;lcd_lvds_mode:      0:NS mode; 1:JEIDA mode
;lcd_lvds_bitwidth:  0:24bit; 1:18bit
;lcd_lvds_io_cross:  0:normal; 1:pn cross
;lcd_cpu_if:         0:18bit; 1:16bit mode0; 2:16bit mode1; 3:16bit mode2; 4:16bit mode3; 5:9bit; 6:8bit 256K; 7:8bit 65K
;lcd_frm:            0:disable; 1:enable rgb666 dither; 2:enable rgb656 dither

;lcd_gpio_0:         SCL
;lcd_gpio_1          SDA
;-------------------------------------------------------------------------------
[lcd0_para]
lcd_used                = 0

lcd_x                   = 800
lcd_y                   = 480
lcd_width           	= 0
lcd_height          	= 0
lcd_dclk_freq           = 33
lcd_pwm_not_used        = 0
lcd_pwm_ch              = 0
lcd_pwm_freq            = 10000
lcd_pwm_pol             = 0
lcd_if                  = 0
lcd_hbp                 = 46
lcd_ht                  = 1055
lcd_vbp                 = 23
lcd_vt                  = 1050
lcd_vspw                = 0
lcd_hspw                = 0
lcd_hv_if               = 0
lcd_hv_smode            = 0
lcd_hv_s888_if          = 0
lcd_hv_syuv_if          = 0
lcd_lvds_ch             = 0
lcd_lvds_mode           = 0
lcd_lvds_bitwidth       = 0
lcd_lvds_io_cross       = 0
lcd_cpu_if              = 0
lcd_frm                 = 0
lcd_io_cfg0             = 0x10000000
lcd_gamma_correction_en = 0
lcd_gamma_tbl_0         = 0x00000000
lcd_gamma_tbl_1         = 0x00010101
lcd_gamma_tbl_255       = 0x00ffffff

lcd_bl_en_used          = 1
lcd_bl_en               = port:PH07<1><0><default><1>

lcd_power_used          = 1
lcd_power               = port:PH08<1><0><default><1>

lcd_pwm_used            = 1
lcd_pwm                 = port:PB02<2><0><default><default>

lcdd0                   = port:PD00<2><0><default><default>
lcdd1                   = port:PD01<2><0><default><default>
lcdd2                   = port:PD02<2><0><default><default>
lcdd3                   = port:PD03<2><0><default><default>
lcdd4                   = port:PD04<2><0><default><default>
lcdd5                   = port:PD05<2><0><default><default>
lcdd6                   = port:PD06<2><0><default><default>
lcdd7                   = port:PD07<2><0><default><default>
lcdd8                   = port:PD08<2><0><default><default>
lcdd9                   = port:PD09<2><0><default><default>
lcdd10                  = port:PD10<2><0><default><default>
lcdd11                  = port:PD11<2><0><default><default>
lcdd12                  = port:PD12<2><0><default><default>
lcdd13                  = port:PD13<2><0><default><default>
lcdd14                  = port:PD14<2><0><default><default>
lcdd15                  = port:PD15<2><0><default><default>
lcdd16                  = port:PD16<2><0><default><default>
lcdd17                  = port:PD17<2><0><default><default>
lcdd18                  = port:PD18<2><0><default><default>
lcdd19                  = port:PD19<2><0><default><default>
lcdd20                  = port:PD20<2><0><default><default>
lcdd21                  = port:PD21<2><0><default><default>
lcdd22                  = port:PD22<2><0><default><default>
lcdd23                  = port:PD23<2><0><default><default>
lcdclk                  = port:PD24<2><0><default><default>
lcdde                   = port:PD25<2><0><default><default>
lcdhsync                = port:PD26<2><0><default><default>
lcdvsync                = port:PD27<2><0><default><default>

;----------------------------------------------------------------------------------
;lcd1 configuration

;lcd_dclk_freq:      in MHZ unit
;lcd_pwm_freq:       in HZ unit
;lcd_if:             0:hv(sync+de); 1:8080; 2:ttl; 3:lvds
;lcd_hbp:            hsync back porch
;lcd_ht:             hsync total cycle
;lcd_vbp:            vsync back porch
;lcd_vt:             vysnc total cycle *2
;lcd_hv_if:          0:hv parallel 1:hv serial
;lcd_hv_smode:       0:RGB888 1:CCIR656
;lcd_hv_s888_if      serial RGB format
;lcd_hv_syuv_if:     serial YUV format
;lcd_hspw:           hsync plus width
;lcd_vspw:           vysnc plus width
;lcd_lvds_ch:        0:single channel; 1:dual channel
;lcd_lvds_mode:      0:NS mode; 1:JEIDA mode
;lcd_lvds_bitwidth:  0:24bit; 1:18bit
;lcd_lvds_io_cross:  0:normal; 1:pn cross
;lcd_cpu_if:         0:18bit; 1:16bit mode0; 2:16bit mode1; 3:16bit mode2; 4:16bit mode3; 5:9bit; 6:8bit 256K; 7:8bit 65K
;lcd_frm:            0:disable; 1:enable rgb666 dither; 2:enable rgb656 dither

;lcd_gpio_0:         SCL
;lcd_gpio_1          SDA
;----------------------------------------------------------------------------------
[lcd1_para]
lcd_used                = 0

lcd_x                   = 0
lcd_y                   = 0
lcd_dclk_freq           = 0
lcd_pwm_not_used        = 0
lcd_pwm_ch              = 1
lcd_pwm_freq            = 0
lcd_pwm_pol             = 0
lcd_if                  = 0
lcd_hbp                 = 0
lcd_ht                  = 0
lcd_vbp                 = 0
lcd_vt                  = 0
lcd_vspw                = 0
lcd_hspw                = 0
lcd_hv_if               = 0
lcd_hv_smode            = 0
lcd_hv_s888_if          = 0
lcd_hv_syuv_if          = 0
lcd_lvds_ch             = 0
lcd_lvds_mode           = 0
lcd_lvds_bitwidth       = 0
lcd_lvds_io_cross       = 0
lcd_cpu_if              = 0
lcd_frm                 = 0
lcd_io_cfg0             = 0
lcd_gamma_correction_en = 0
lcd_gamma_tbl_0         = 0x00000000
lcd_gamma_tbl_1         = 0x00010101
lcd_gamma_tbl_255       = 0x00ffffff

lcd_bl_en_used          = 0
lcd_bl_en               =

lcd_power_used          = 0
lcd_power               =

lcd_pwm_used            = 1
lcd_pwm                 = port:PI03<2><0><default><default>

lcd_gpio_0              =
lcd_gpio_1              =
lcd_gpio_2              =
lcd_gpio_3              =

lcdd0                   = port:PH00<2><0><default><default>
lcdd1                   = port:PH01<2><0><default><default>
lcdd2                   = port:PH02<2><0><default><default>
lcdd3                   = port:PH03<2><0><default><default>
lcdd4                   = port:PH04<2><0><default><default>
lcdd5                   = port:PH05<2><0><default><default>
lcdd6                   = port:PH06<2><0><default><default>
lcdd7                   = port:PH07<2><0><default><default>
lcdd8                   = port:PH08<2><0><default><default>
lcdd9                   = port:PH09<2><0><default><default>
lcdd10                  = port:PH10<2><0><default><default>
lcdd11                  = port:PH11<2><0><default><default>
lcdd12                  = port:PH12<2><0><default><default>
lcdd13                  = port:PH13<2><0><default><default>
lcdd14                  = port:PH14<2><0><default><default>
lcdd15                  = port:PH15<2><0><default><default>
lcdd16                  = port:PH16<2><0><default><default>
lcdd17                  = port:PH17<2><0><default><default>
lcdd18                  = port:PH18<2><0><default><default>
lcdd19                  = port:PH19<2><0><default><default>
lcdd20                  = port:PH20<2><0><default><default>
lcdd21                  = port:PH21<2><0><default><default>
lcdd22                  = port:PH22<2><0><default><default>
lcdd23                  = port:PH23<2><0><default><default>
lcdclk                  = port:PH24<2><0><default><default>
lcdde                   = port:PH25<2><0><default><default>
lcdhsync                = port:PH26<2><0><default><default>
lcdvsync                = port:PH27<2><0><default><default>

;-------------------------------------------------------------------------------
;tv out dac configuration
;dacx_src:  0:composite; 1:luma; 2:chroma; 4:Y; 5:Pb; 6: Pr; 7:none
;-------------------------------------------------------------------------------
[tv_out_dac_para]
dac_used                = 1
dac0_src                = 4
dac1_src                = 5
dac2_src                = 6
dac3_src                = 0

;----------------------------------------------------------------------------------
;hdmi configuration
;----------------------------------------------------------------------------------
[hdmi_para]
hdmi_used               = 1
hdcp_enable             = 0

[i2s2_para]
i2s_channel             = 2
i2s_master              = 4
i2s_select              = 1
audio_format            = 1
signal_inversion        = 1
over_sample_rate        = 256
sample_resolution       = 16
word_select_size        = 32
pcm_sync_period         = 256
msb_lsb_first           = 0
sign_extend             = 0
slot_index              = 0
slot_width              = 16
frame_width             = 1
tx_data_mode            = 0
rx_data_mode            = 0
;i2s_mclk            	= port:PB05<2><1><default><default>
;i2s_bclk            	= port:PB06<2><1><default><default>
;i2s_lrclk           	= port:PB07<2><1><default><default>
;i2s_dout0           	= port:PB08<2><1><default><default>
;i2s_dout1           	=
;i2s_dout2           	=
;i2s_dout3           	=
;i2s_din             	= port:PB12<2><1><default><default>


[camera_list_para]
camera_list_para_used   = 1
ov7670                  = 0
gc0308                  = 0
gt2005                  = 0
hi704                   = 0
sp0838                  = 0
mt9m112                 = 0
mt9m113                 = 0
ov2655                  = 0
hi253                   = 0
gc0307                  = 0
mt9d112                 = 0
ov5640                  = 0
gc2015                  = 0
ov2643                  = 0
gc0329                  = 0
gc0309                  = 0
tvp5150                 = 0
s5k4ec                  = 0
ov5650_mv9335           = 0
siv121d                 = 0
nt99141			= 1

;--------------------------------------------------------------------------------
;csi gpio configuration
;csi_if: 0:hv_8bit 1:hv_16bit 2:hv_24bit 3:bt656 1ch 4:bt656 2ch 5:bt656 4ch
;csi_mode: 0:sample one csi to one buffer 1:sample two csi to one buffer
;csi_dev_qty: The quantity of devices linked to csi interface
;csi_vflip: flip in vertical direction 0:disable 1:enable
;csi_hflip: flip in horizontal direction 0:disable 1:enable
;csi_stby_mode: 0:not shut down power at standby 1:shut down power at standby
;csi_iovdd: camera module io power , pmu power supply
;csi_avdd:	camera module analog power , pmu power supply
;csi_dvdd:	camera module core power , pmu power supply
;pmu_ldo3:  fill "axp20_pll"
;pmu_ldo4:  fill "axp20_hdmi"
;fill "" when not using any pmu power supply
;csi_flash_pol: the active polority of the flash light IO 0:low active 1:high active
;--------------------------------------------------------------------------------

[csi0_para]
csi_used            = 1

csi_dev_qty         = 1
csi_stby_mode       = 0
csi_mname           = "nt99141"
csi_if              = 0
csi_iovdd           = "axp20_pll"
csi_avdd            = ""
csi_dvdd            = ""
csi_vol_iovdd       = 2800
csi_vol_dvdd        = 
csi_vol_avdd        = 
csi_vflip           = 1
csi_hflip           = 0
csi_flash_pol       = 0
csi_facing          = 0

csi_twi_id          = 1
csi_twi_addr        = 0x54
csi_pck             = port:PE00<3><default><default><default>
csi_ck              = port:PE01<3><default><default><default>
csi_hsync           = port:PE02<3><default><default><default>
csi_vsync           = port:PE03<3><default><default><default>
csi_d0              = port:PE04<3><default><default><default>
csi_d1              = port:PE05<3><default><default><default>
csi_d2              = port:PE06<3><default><default><default>
csi_d3              = port:PE07<3><default><default><default>
csi_d4              = port:PE08<3><default><default><default>
csi_d5              = port:PE09<3><default><default><default>
csi_d6              = port:PE10<3><default><default><default>
csi_d7              = port:PE11<3><default><default><default>
csi_reset           = port:PH14<1><default><default><0>
csi_power_en        = port:PH16<1><default><default><0>
csi_stby            = port:PH19<1><default><default><0>

[csi1_para]
csi_used            = 0

csi_dev_qty         = 1
csi_stby_mode       = 0
csi_mname           = "gc0308"
csi_if              = 0
csi_iovdd           = ""
csi_avdd            = ""
csi_dvdd            = ""
csi_vol_iovdd       = 
csi_vol_dvdd        = 
csi_vol_avdd        = 
csi_vflip           = 0
csi_hflip           = 0
csi_flash_pol       = 0
csi_facing          = 1

csi_twi_id          = 1
csi_twi_addr        = 0x42
csi_pck             = port:PG00<3><default><default><default>
csi_ck              = port:PG01<3><default><default><default>
csi_hsync           = port:PG02<3><default><default><default>
csi_vsync           = port:PG03<3><default><default><default>
csi_d0              = port:PG04<3><default><default><default>
csi_d1              = port:PG05<3><default><default><default>
csi_d2              = port:PG06<3><default><default><default>
csi_d3              = port:PG07<3><default><default><default>
csi_d4              = port:PG08<3><default><default><default>
csi_d5              = port:PG09<3><default><default><default>
csi_d6              = port:PG10<3><default><default><default>
csi_d7              = port:PG11<3><default><default><default>
csi_reset           = port:PH14<1><default><default><0>
csi_power_en        = 
csi_stby            = port:PH17<1><default><default><0>

;-------------------------------------------------------------------------------
;tv configuration
;
;-------------------------------------------------------------------------------
[tvout_para]
tvout_used          = 1
tvout_channel_num   = 1

[tvin_para]
tvin_used           = 0
tvin_channel_num    = 4


;-------------------------------------------------------------------------------
;sata configuration
;
;-------------------------------------------------------------------------------
[sata_para]
sata_used           = 0
sata_power_en       = 


;-------------------------------------------------------------------------------
;   SDMMC PINS MAPPING
; ------------------------------------------------------------------------------
;   Config Guide
;   sdc_used: 1-enable card, 0-disable card
;   sdc_detmode: card detect mode
;                1-detect card by gpio polling
;                2-detect card by gpio irq(must use IO with irq function)
;                3-no detect, always in for boot card
;                4-manually insert and remove by /proc/driver/sunxi-mmc.x/insert
;   sdc_buswidth: card bus width, 1-1bit, 4-4bit, 8-8bit
;   sdc_use_wp: 1-with write protect IO, 0-no write protect IO
;   sdc_isio: for sdio card
;   sdc_regulator: power control.
;   other: GPIO Mapping configuration
; ------------------------------------------------------------------------------
;   Note:
;   1 if detmode=2, sdc_det's config=6
;     else if detmode=1, sdc_det's config=0
;     else sdc_det IO is not necessary
;   2 if the customer wants to support UHS-I and HS200 features, he must provide
;     an independent power supply for the card. This is only used in platforms
;     that supports SD3.0 cards and eMMC4.4+ flashes
;-------------------------------------------------------------------------------
[mmc0_para]
sdc_used            = 1
sdc_detmode         = 3
sdc_buswidth        = 4
sdc_clk             = port:PF02<2><1><2><default>
sdc_cmd             = port:PF03<2><1><2><default>
sdc_d0              = port:PF01<2><1><2><default>
sdc_d1              = port:PF00<2><1><2><default>
sdc_d2              = port:PF05<2><1><2><default>
sdc_d3              = port:PF04<2><1><2><default>
sdc_det             = port:PH10<0><1><default><default>
sdc_use_wp          = 0
sdc_wp              =
sdc_isio            = 0
sdc_regulator       = "none"

[mmc1_para]
sdc_used            = 0
sdc_detmode         = 4
sdc_buswidth        = 4
sdc_clk             = port:PG00<2><1><2><default>
sdc_cmd             = port:PG01<2><1><2><default>
sdc_d0              = port:PG02<2><1><2><default>
sdc_d1              = port:PG03<2><1><2><default>
sdc_d2              = port:PG04<2><1><2><default>
sdc_d3              = port:PG05<2><1><2><default>
sdc_det             =
sdc_use_wp          = 0
sdc_wp              =
sdc_isio            = 0
sdc_regulator       = "none"

[mmc2_para]
sdc_used            = 0
sdc_detmode         = 3
sdc_buswidth        = 4
sdc_cmd             = port:PC06<3><1><2><default>
sdc_clk             = port:PC07<3><1><2><default>
sdc_d0              = port:PC08<3><1><2><default>
sdc_d1              = port:PC09<3><1><2><default>
sdc_d2              = port:PC10<3><1><2><default>
sdc_d3              = port:PC11<3><1><2><default>
sdc_det             =
sdc_use_wp          = 0
sdc_wp              =
sdc_isio            = 0
sdc_regulator       = "none"

[mmc3_para]
sdc_used            = 1
sdc_detmode         = 4
sdc_buswidth        = 4
sdc_cmd             = port:PI04<2><1><2><default>
sdc_clk             = port:PI05<2><1><2><default>
sdc_d0              = port:PI06<2><1><2><default>
sdc_d1              = port:PI07<2><1><2><default>
sdc_d2              = port:PI08<2><1><2><default>
sdc_d3              = port:PI09<2><1><2><default>
sdc_det             =
sdc_use_wp          = 0
sdc_wp              =
sdc_isio            = 1
sdc_regulator       = "none"

; ------------------------------------------------------------------------------
; memory stick configuration
;-------------------------------------------------------------------------------
[ms_para]
ms_used             = 0
ms_bs               = port:PH06<5><default><default><default>
ms_clk              = port:PH07<5><default><default><default>
ms_d0               = port:PH08<5><default><default><default>
ms_d1               = port:PH09<5><default><default><default>
ms_d2               = port:PH10<5><default><default><default>
ms_d3               = port:PH11<5><default><default><default>
ms_det              =

; ------------------------------------------------------------------------------
; sim card configuration
;-------------------------------------------------------------------------------
[smc_para]
smc_used            = 0
smc_rst             = port:PH13<5><default><default><default>
smc_vppen           = port:PH14<5><default><default><default>
smc_vppp            = port:PH15<5><default><default><default>
smc_det             = port:PH16<5><default><default><default>
smc_vccen           = port:PH17<5><default><default><default>
smc_sck             = port:PH18<5><default><default><default>
smc_sda             = port:PH19<5><default><default><default>

;-------------------------------------------------------------------------------
;ps2 configuration
;-------------------------------------------------------------------------------
[ps2_0_para]
ps2_used            = 0
ps2_scl             = port:PI20<2><1><default><default>
ps2_sda             = port:PI21<2><1><default><default>

[ps2_1_para]
ps2_used            = 0
ps2_scl             = port:PI14<3><1><default><default>
ps2_sda             = port:PI15<3><1><default><default>

;-------------------------------------------------------------------------------
;can bus configuration
;-------------------------------------------------------------------------------
[can_para]
can_used = 0
can_tx              = port:PA16<3><default><default><default>
can_rx              = port:PA17<3><default><default><default>

;-------------------------------------------------------------------------------
;key matrix
;-------------------------------------------------------------------------------
[keypad_para]
kp_used             = 0
kp_in_size          = 8
kp_out_size         = 8
kp_in0              = port:PH08<4><1><default><default>
kp_in1              = port:PH09<4><1><default><default>
kp_in2              = port:PH10<4><1><default><default>
kp_in3              = port:PH11<4><1><default><default>
kp_in4              = port:PH14<4><1><default><default>
kp_in5              = port:PH15<4><1><default><default>
kp_in6              = port:PH16<4><1><default><default>
kp_in7              = port:PH17<4><1><default><default>
kp_out0             = port:PH18<4><1><default><default>
kp_out1             = port:PH19<4><1><default><default>
kp_out2             = port:PH22<4><1><default><default>
kp_out3             = port:PH23<4><1><default><default>
kp_out4             = port:PH24<4><1><default><default>
kp_out5             = port:PH25<4><1><default><default>
kp_out6             = port:PH26<4><1><default><default>
kp_out7             = port:PH27<4><1><default><default>


;-------------------------------------------------------------------------------
;[usbc0]：控制器0的配置。
;usb_used：USB使能标志。置1，表示系统中USB模块可用,置0,则表示系统USB禁用。
;usb_port_type：USB端口的使用情况。 0：device only;1：host only;2：OTG
;usb_detect_type：USB端口的检查方式。0：不做检测;1：vbus/id检查;2：id/dpdm检查
;usb_id_gpio：USB ID pin脚配置。具体请参考gpio配置说明。
;usb_det_vbus_gpio：USB DET_VBUS pin脚配置。具体请参考gpio配置说明。
;usb_drv_vbus_gpio：USB DRY_VBUS pin脚配置。具体请参考gpio配置说明。
;usb_det_vbus_gpio: "axp_ctrl",表示axp 提供
;usb_restrict_gpio  usb限流控制pin
;usb_restric_flag:  usb限流标置
;-------------------------------------------------------------------------------
;-------------------------------------------------------------------------------
;---       USB0控制标志
;-------------------------------------------------------------------------------
[usbc0]
usb_used            = 1
usb_port_type       = 2
usb_detect_type     = 1
usb_id_gpio         = port:PH04<0><1><default><default>
usb_det_vbus_gpio   = "axp_ctrl"
usb_drv_vbus_gpio   = port:PB09<1><0><default><0>
usb_restrict_gpio   = port:PH00<1><0><default><0>
usb_host_init_state = 0
usb_restric_flag    = 0
usb_restric_voltage = 3550000
usb_restric_capacity= 5

;-------------------------------------------------------------------------------
;---       USB1控制标志
;------------------------------------------------------------------------------
[usbc1]
usb_used            = 1
usb_port_type       = 1
usb_detect_type     = 0
;usb_drv_vbus_gpio   = port:PH03<1><0><default><0>
usb_restrict_gpio   = 
usb_host_init_state = 1
usb_restric_flag    = 0

;------------------------------------------------------------------------------
;---       USB2控制标志
;------------------------------------------------------------------------------
[usbc2]
usb_used            = 1
usb_port_type       = 1
usb_detect_type     = 0
usb_drv_vbus_gpio   = port:PH03<1><0><default><0>
usb_restrict_gpio   =
usb_host_init_state = 1
usb_restric_flag    = 0

;--------------------------------
;---       USB Device
;--------------------------------
[usb_feature]
vendor_id           = 0x18D1
mass_storage_id     = 0x0001
adb_id              = 0x0002

manufacturer_name   = "USB Developer"
product_name        = "BPI-QBOX"
serial_number       = "20080411"

[msc_feature]
vendor_name         = "USB 2.0"
product_name        = "USB Flash Driver"
release             = 100
luns                = 3

;-------------------------------------------------------------------------------
; G sensor configuration
; gs_twi_id ---  TWI ID for controlling Gsensor (0: TWI0, 1: TWI1, 2: TWI2)
;-------------------------------------------------------------------------------
[gsensor_para]
gsensor_used         = 0
gsensor_twi_id       = 1
gsensor_int1         = 
gsensor_int2         = 

;--------------------------------------------------------------------------------
; G sensor automatic detection configuration
;gsensor_detect_used  --- Whether startup automatic inspection function. 1:used,0:unused
;Module name postposition 1 said detection, 0 means no detection. 
;--------------------------------------------------------------------------------
[gsensor_list_para]
gsensor_det_used          = 0
bma250                    = 1
mma8452                   = 1
mma7660                   = 1
mma865x                   = 1
afa750                    = 1
lis3de_acc                = 1
lis3dh_acc                = 1
kxtik                     = 1
dmard10                   = 0
dmard06                   = 1
mxc622x                   = 1
fxos8700                  = 1
lsm303d                   = 1

;-------------------------------------------------------------------------------
; gps gpio configuration
; gps_spi_id        --- the index of SPI controller. 0: SPI0, 1: SPI1, 2: SPI2, 15: no SPI used
; gps_spi_cs_num    --- the chip select number of SPI controller. 0: SPI CS0, 1: SPI CS1
; gps_lradc         --- the lradc number for GPS used. 0 and 1 is valid, set 2 if not use lradc
;-------------------------------------------------------------------------------
[gps_para]
gps_used            = 0
gps_spi_id          = 2
gps_spi_cs_num      = 0
gps_lradc           = 1
gps_clk             = port:PI00<2><default><default><default>
gps_sign            = port:PI01<2><default><default><default>
gps_mag             = port:PI02<2><default><default><default>
gps_vcc_en          = port:PC22<1><default><default><0>
gps_osc_en          = port:PI14<1><default><default><0>
gps_rx_en           = port:PI15<1><default><default><0>

;--------------------------------------------------------------------------------
;wifi configuration
;wifi_sdc_id    ---  0- SDC0, 1- SDC1, 2- SDC2, 3- SDC3
;wifi_usbc_id  ---  0- USB0, 1- USB1, 2- USB2
;wifi_usbc_type --  1- EHCI(speed 2.0), 2- OHCI(speed 1.0)
;wifi_mod_sel   ---  0- none, 1- bcm40181, 2- bcm40183(wifi+bt),
;                    3 - rtl8723as(wifi+bt), 4- rtl8189es(SM89E00),
;                    5 - rtl8192cu, 6 - rtl8188eu, 7 - ap6210,
;		     8 - ap6330, 9 - ap6181, 10 - ap6335, 
;		     11 - rtl8723au, 12 - rtl8821au,
;--------------------------------------------------------------------------------
[wifi_para]
wifi_used          = 1
wifi_sdc_id        = 3
wifi_usbc_id       = 2
wifi_usbc_type     = 1
wifi_mod_sel       = 10
wifi_power         = ""

; 1 - bcm40181 sdio wifi gpio config
;bcm40181_shdn          = port:PH09<1><default><default><0>
;bcm40181_host_wake     = port:PH10<0><default><default><0>

; 2 - bcm40183 sdio wifi gpio config
;bcm40183_wl_regon      = port:PH09<1><default><default><0>
;bcm40183_wl_host_wake  = port:PH10<0><default><default><0>
;bcm40183_bt_rst        = port:PB05<1><default><default><0>
;bcm40183_bt_regon      = port:PB05<1><default><default><0>
;bcm40183_bt_wake       = port:PI15<1><default><default><0>
;bcm40183_bt_host_wake  = port:PI21<0><default><default><0>

; 3 - rtl8723as sdio wifi + bt gpio config
;rtk_rtl8723as_wl_dis       = port:PH09<1><default><default><0>
;rtk_rtl8723as_bt_dis       = port:PB05<1><default><default><0>
;rtk_rtl8723as_wl_host_wake = port:PH10<0><default><default><0>
;rtk_rtl8723as_bt_host_wake = port:PI21<0><default><default><0>

; 4 - rtl8189es sdio wifi gpio config
;rtl8189es_shdn         = port:PH09<1><default><default><0>
;rtl8189es_wakeup       = port:PH10<1><default><default><1>

; 5 - rtl8192cu usb wifi
rtl8192cu_wl_regon	= port:PG05<1><default><default><0>

; 7 - rtl8821au usb wifi
rtl8821au_wl_regon	 = port:PG04<1><default><default><0>

; 6 - rtl8188eu usb wifi
;rtk_rtl8188eu_wl_dis    = port:PH03<1><default><default><0>


; 7 - ap6xxx sdio wifi + bt gpio config
ap6xxx_wl_regon      = port:PH22<1><default><default><0>
ap6xxx_wl_host_wake  = port:PH15<6><default><default><default>
ap6xxx_bt_regon      = port:PB05<1><default><default><0>
ap6xxx_bt_wake       = port:PI20<1><default><default><0>
ap6xxx_bt_host_wake  = port:PI15<6><default><default><default>
ap6xxx_lpo	     = port:PI12<0><default><default><0>
ap6xxx_wl_host_wake_invert = 0

;-------------------------------------------------------------------------------
;3G configuration
;-------------------------------------------------------------------------------
[3g_para]
3g_used             = 0
3g_usbc_num         = 2
3g_uart_num         = 0
3g_pwr              =
3g_wakeup           =
3g_int              =

;--------------------------------------------------------------------------------
;modem configuration
;--------------------------------------------------------------------------------
[modem_para]
modem_used	= 1
modem_usbc_num	= 2
modem_uart_num	= 5
modem_name		= "sim7100c"
modem_vbat		= port:PG02<1><default><default><0>
modem_pwr_on		= port:PG01<1><default><default><0>
modem_wake		= 
modem_rf_dis		= port:PG00<1><default><default><0>
modem_rst		= port:PG03<1><default><default><0>
modem_dldo         	= ""
modem_dldo_min_uV	= 
modem_dldo_max_uV	= 

;-------------------------------------------------------------------------------
;gyroscope
;-------------------------------------------------------------------------------
[gy_para]
gy_used             = 0
gy_twi_id           = 1
gy_twi_addr         = 0x00
gy_int1             = port:PH18<6><1><default><default>
gy_int2             = port:PH19<6><1><default><default>

;-------------------------------------------------------------------------------
;light sensor
;-------------------------------------------------------------------------------
[ls_para]
ls_used             = 0
ls_twi_id           = 1
ls_twi_addr         = 0x00
ls_int              = port:PH20<6><1><default><default>

;-------------------------------------------------------------------------------
;compass
;-------------------------------------------------------------------------------
[compass_para]
compass_used        = 0
compass_twi_id      = 1
compass_twi_addr    = 0x00
compass_int         = port:PI13<6><1><default><default>

;-------------------------------------------------------------------------------
;blue tooth
;bt_used            ---- blue tooth used (0- no used, 1- used)
;bt_uard_id         ---- uart index
;-------------------------------------------------------------------------------
[bt_para]
bt_used             = 1
bt_uart_id          = 3
bt_wake             = port:PI20<1><default><default><default>
bt_host_wake  	    = port:PH02<6><default><default><default>
bt_gpio             = port:PI21<1><default><default><default>
bt_rst              = port:PB05<1><default><default><default>
bt_host_wake_invert   = 0
;bt_wake_invert	    = 0

[i2s_para]
i2s_used            = 0
i2s_channel         = 2
i2s_mclk            = port:PB5<2><1><default><default>
i2s_bclk            = port:PB6<2><1><default><default>
i2s_lrclk           = port:PB7<2><1><default><default>
i2s_dout0           = port:PB8<2><1><default><default>
i2s_dout1           =
i2s_dout2           =
i2s_dout3           =
i2s_din             = port:PB12<2><1><default><default>


;--------------------------------------------------------------------------------
;pcm_master:1: SND_SOC_DAIFMT_CBM_CFM(codec clk & FRM master)        use
;			2: SND_SOC_DAIFMT_CBS_CFM(codec clk slave & FRM master)  not use
;			3: SND_SOC_DAIFMT_CBM_CFS(codec clk master & frame slave) not use
;			4: SND_SOC_DAIFMT_CBS_CFS(codec clk & FRM slave)         use
;pcm_select:1 is pcm.0 is i2s
;audio_format: 1:SND_SOC_DAIFMT_I2S(standard i2s format).            use
;			   2:SND_SOC_DAIFMT_RIGHT_J(right justfied format).
;			   3:SND_SOC_DAIFMT_LEFT_J(left justfied format)
;			   4:SND_SOC_DAIFMT_DSP_A(pcm. MSB is available on 2nd BCLK rising edge after LRC rising edge). use
;			   5:SND_SOC_DAIFMT_DSP_B(pcm. MSB is available on 1nd BCLK rising edge after LRC rising edge)
;signal_inversion:1:SND_SOC_DAIFMT_NB_NF(normal bit clock + frame)  use
;				  2:SND_SOC_DAIFMT_NB_IF(normal BCLK + inv FRM)
;				  3:SND_SOC_DAIFMT_IB_NF(invert BCLK + nor FRM)  use
;				  4:SND_SOC_DAIFMT_IB_IF(invert BCLK + FRM)
;over_sample_rate: support 128fs/192fs/256fs/384fs/512fs/768fs
;sample_resolution	:16bits/20bits/24bits
;word_select_size 	:16bits/20bits/24bits/32bits
;pcm_sync_period 	:16/32/64/128/256
;msb_lsb_first 		:0: msb first; 1: lsb first
;sign_extend 		:0: zero pending; 1: sign extend
;slot_index 		:slot index: 0: the 1st slot - 3: the 4th slot
;slot_width 		:8 bit width / 16 bit width
;frame_width 		:0: long frame = 2 clock width;  1: short frame
;tx_data_mode 		:0: 16bit linear PCM; 1: 8bit linear PCM; 2: 8bit u-law; 3: 8bit a-law
;rx_data_mode 		:0: 16bit linear PCM; 1: 8bit linear PCM; 2: 8bit u-law; 3: 8bit a-law
;--------------------------------------------------------------------------------
[pcm_para]
pcm_used            = 0
pcm_channel         = 2
pcm_master		= 4
pcm_select  		= 1
audio_format		= 4
signal_inversion	= 1
over_sample_rate	= 256
sample_resolution   = 16
word_select_size 	= 32
pcm_sync_period 	= 256
msb_lsb_first 	    = 0
sign_extend 		= 0
slot_index 		    = 0
slot_width 		    = 16
frame_width 		= 1
tx_data_mode 		= 0
rx_data_mode 		= 0
pcm_mclk            = port:PA09<6><1><default><default>
pcm_bclk            = port:PA14<6><1><default><default>
pcm_lrclk           = port:PA15<6><1><default><default>
pcm_dout0           = port:PA16<6><1><default><default>
pcm_dout1           =
pcm_dout2           =
pcm_dout3           =
pcm_din             = port:PA17<6><1><default><default>

;--------------------------------------------------------------------------------
;spdif_used		:0: not use spdif; 1: use spdif
;spdif_mclk		:spdif mclk pin
;spdif_dout		:spdif output pin
;spdif_din		:spdif input pin
;--------------------------------------------------------------------------------
[spdif_para]
spdif_used          = 0
spdif_mclk          =
spdif_dout          = port:PB13<4><1><default><default>
spdif_din           =

;--------------------------------------------------------------------------------
;audio_used 			:0: not use audio; 1: use audio
;audio_pa_ctrl_used		:0: not use audio_pa_ctrl; 1: use audio_pa_ctrl
;audio_pa_ctrl			:audio_pa_ctrl pin
;--------------------------------------------------------------------------------
[audio_para]
audio_used          = 1
audio_pa_ctrl_used  = 1
audio_pa_ctrl       = port:PH05<1><default><default><1>

[switch_para]
switch_used=1

;-------------------------------------------------------------------------------
;ir --- infra remote configuration
;支持无MCU板子红外唤醒 ir_wakeup=1 有MCU设为0
;-------------------------------------------------------------------------------
[ir_para]
ir_used             = 1
ir_rx               = port:PB04<2><default><default><default>
ir_wakeup           = 1
power_key           = 0x74
ir_addr_code        = 0xc617

;----------------------------------------------------------------------------------                                   
;gpio configuration
;gpio_pin_1               = port:PB21<1><default><default><0>
;gpio_pin_2               = port:PB20<1><default><default><0>
;gpio_pin_3               = port:PI03<1><default><default><0>
;gpio_pin_4               = port:PG10<1><default><default><0>
;gpio_pin_5               = port:PG11<1><default><default><0>
;gpio_pin_6               = port:PI19<1><default><default><0>
;gpio_pin_7               = port:PH02<1><default><default><0>
;gpio_pin_8               = port:PI18<1><default><default><0>
;gpio_pin_9               = port:PI17<1><default><default><0>
;gpio_pin_10              = port:PH20<1><default><default><0>
;gpio_pin_11              = port:PH21<1><default><default><0>
;gpio_pin_12              = port:PI12<1><default><default><0>
;gpio_pin_13              = port:PI13<1><default><default><0>
;gpio_pin_14              = port:PI16<1><default><default><0>
;gpio_pin_15              = port:PI11<1><default><default><0>
;gpio_pin_16              = port:PI10<1><default><default><0>
;gpio_pin_17              = port:PI14<1><default><default><0>
;gpio_pin_18              = port:PH24<1><default><default><0>
;gpio_pin_19              = port:PH25<1><default><default><0>
;gpio_pin_20              = port:PH26<1><default><default><0>
;gpio_pin_21              = port:PH27<1><default><default><0>
;----------------------------------------------------------------------------------
[gpio_para]                    
gpio_used                = 1
gpio_num                 = 26
gpio_pin_1               = port:PB21<1><default><default><0>
gpio_pin_2               = port:PB20<1><default><default><0>
gpio_pin_3               = port:PI03<1><default><default><0>
gpio_pin_4               = port:PG10<1><default><default><0>
gpio_pin_5               = port:PG11<1><default><default><0>
gpio_pin_6               = port:PI19<1><default><default><0>
gpio_pin_7               = port:PH02<1><default><default><0>
gpio_pin_8               = port:PI18<1><default><default><0>
gpio_pin_9               = port:PI17<1><default><default><0>
gpio_pin_10              = port:PH20<1><default><default><0>
gpio_pin_11              = port:PH21<1><default><default><0>
gpio_pin_12              = port:PB02<1><default><default><0>
gpio_pin_13              = port:PI13<1><default><default><0>
gpio_pin_14              = port:PI16<1><default><default><0>
gpio_pin_15              = port:PI11<1><default><default><0>
gpio_pin_16              = port:PI10<1><default><default><0>
gpio_pin_17              = port:PI14<1><default><default><0>
gpio_pin_18              = port:PH24<1><default><default><0>
gpio_pin_19              = port:PH25<1><default><default><0>
gpio_pin_20              = port:PH26<1><default><default><0>
gpio_pin_21              = port:PH27<1><default><default><0>
gpio_pin_22              = port:PH06<1><default><default><1>
gpio_pin_23              = port:PH07<1><default><default><1>
gpio_pin_24              = port:PH08<1><default><default><1>
gpio_pin_25              = port:PH17<1><default><default><0>
gpio_pin_26              = port:PH18<1><default><default><0>


;-------------------------------------------------------------------------------
;pmu_twi_addr           ---slave address
;pmu_twi_id             ---i2c bus number (0 TWI0, 1 TWI2, 2 TWI3)
;pmu_irq_id             ---irq number (0 irq0,1 irq1,……)
;pmu_battery_rdc        ---battery initial resistance,mΩ,根据实际电池内阻填写
;pmu_battery_cap        ---battery capability,mAh，根据实际电池容量填写
;pmu_init_chgcur        ---set initial charging current limite,mA，300/400/500/600/700/800/900/1000/1100/1200/1300/1400/1500/1600/1700/1800
;pmu_suspend_chgcur     ---set suspend charging current limite,mA，300/400/500/600/700/800/900/1000/1100/1200/1300/1400/1500/1600/1700/1800
;pmu_resume_chgcur      ---set resume charging current limite,mA，300/400/500/600/700/800/900/1000/1100/1200/1300/1400/1500/1600/1700/1800
;pmu_shutdown_chgcur    ---set shutdown charging current limite,mA，300/400/500/600/700/800/900/1000/1100/1200/1300/1400/1500/1600/1700/1800
;pmu_init_chgvol        ---set initial charing target voltage,mV,4100/4150/4200/4360
;pmu_init_chgend_rate   ---set initial charing end current  rate,10/15
;pmu_init_chg_enabled   ---set initial charing enabled,0:关闭,1:打开
;pmu_init_adc_freq      ---set initial adc frequency,Hz,25/50/100/200
;pmu_init_adc_freqc     ---set initial coulomb adc coufrequency,Hz,25/50/100/200
;pmu_init_chg_pretime   ---set initial pre-charging time,min,40/50/60/70
;pmu_init_chg_csttime   ---set initial constance-charging time,min,360/480/600/720
;pmu_bat_para1 		    ---battery indication at 3.1328V
;pmu_bat_para2          ---battery indication at 3.2736V
;pmu_bat_para3          ---battery indication at 3.4144V
;pmu_bat_para4          ---battery indication at 3.5552V
;pmu_bat_para5          ---battery indication at 3.6256V
;pmu_bat_para6          ---battery indication at 3.6608V
;pmu_bat_para7          ---battery indication at 3.6960V
;pmu_bat_para8          ---battery indication at 3.7312V
;pmu_bat_para9          ---battery indication at 3.7664V
;pmu_bat_para10         ---battery indication at 3.8016V
;pmu_bat_para11         ---battery indication at 3.8368V
;pmu_bat_para12         ---battery indication at 3.8720V
;pmu_bat_para13         ---battery indication at 3.9424V
;pmu_bat_para14         ---battery indication at 4.0128V
;pmu_bat_para15         ---battery indication at 4.0832V
;pmu_bat_para16         ---battery indication at 4.1536V
;pmu_usbvol             ---set usb-ac limited voltage level,mV,4000/4100/4200/4300/4400/4500/4600/4700,0 - not limite
;pmu_usbcur             ---set usb-ac limited voltage level,mA,100/500/900, 0 - not limite
;pmu_usbvol_pc	        ---set usb-pc limited voltage level,mV,4000/4100/4200/4300/4400/4500/4600/4700,0 - not limite
;pmu_usbcur_pc          ---set usb-pc limited voltage level,mA,100/500/900, 0 - not limite
;pmu_pwroff_vol         ---set protect voltage when system start up,mV,2600/2700/2800/2900/3000/3100/3200/3300
;pmu_pwron_vol          ---set protect voltage after system start up,mV,2600/2700/2800/2900/3000/3100/3200/3300
;pmu_pekoff_time        ---set pek off time,ms, 4000/6000/8000/10000
;pmu_pekoff_en          ---set pek off enable, 0:关闭,1:打开
;pmu_peklong_time       ---set pek pek long irq time,ms,1000/1500/2000/2500
;pmu_pekon_time         ---set pek on time,ms,128/1000/2000/3000
;pmu_pwrok_time         ---set pmu pwrok delay time,ms,8/64
;pmu_pwrnoe_time        ---set pmu n_oe power down delay time,ms,128/1000/2000/3000
;pmu_intotp_en		  	---set pmu power down when overtempertur enable,0:关闭，1：打开
;pmu_suspendpwroff_vol  ---set pmu shutdown voltage when cpu is suspend and battery voltage is low
;pmu_batdeten			---set pmu battery detect enabled,0:关闭，1：打开
;-------------------------------------------------------------------------------
[pmu_para]
pmu_used                 = 1
pmu_twi_addr             = 0x34
pmu_twi_id               = 0
pmu_irq_id               = 32
pmu_battery_rdc          = 100
pmu_battery_cap          = 3200
pmu_init_chgcur          = 300
pmu_earlysuspend_chgcur  = 600
pmu_suspend_chgcur       = 1000
pmu_resume_chgcur        = 300
pmu_shutdown_chgcur      = 1000
pmu_init_chgvol          = 4200
pmu_init_chgend_rate     = 15
pmu_init_chg_enabled     = 1
pmu_init_adc_freq        = 100
pmu_init_adc_freqc       = 100
pmu_init_chg_pretime     = 50
pmu_init_chg_csttime     = 720
power_start		 = 3

pmu_bat_para1            = 0
pmu_bat_para2            = 0
pmu_bat_para3            = 0
pmu_bat_para4            = 0
pmu_bat_para5            = 5
pmu_bat_para6            = 8
pmu_bat_para7            = 11
pmu_bat_para8            = 22
pmu_bat_para9            = 33
pmu_bat_para10           = 43
pmu_bat_para11           = 50
pmu_bat_para12           = 59
pmu_bat_para13           = 71
pmu_bat_para14           = 83
pmu_bat_para15           = 92
pmu_bat_para16           = 100

pmu_usbvol_limit         = 1
pmu_usbcur_limit         = 0
pmu_usbvol               = 4000
pmu_usbcur               = 0

pmu_usbvol_pc            = 4400
pmu_usbcur_pc            = 0

pmu_pwroff_vol           = 3300
pmu_pwron_vol            = 2900

pmu_pekoff_time          = 10000
pmu_pekoff_en            = 1
pmu_peklong_time         = 1500
pmu_pekon_time           = 1000
pmu_pwrok_time           = 64
pmu_pwrnoe_time          = 2000
pmu_intotp_en            = 1

pmu_used2                = 0
pmu_adpdet               = port:PH02<0><default><default><default>
pmu_init_chgcur2         = 400
pmu_earlysuspend_chgcur2 = 600
pmu_suspend_chgcur2      = 1200
pmu_resume_chgcur2       = 400
pmu_shutdown_chgcur2     = 1200

pmu_suspendpwroff_vol    = 3500

pmu_batdeten             = 1

[recovery_key]
key_min			=4
key_max			=6


;----------------------------------------------------------------------------------
;		recovery_key  : 一键恢复（通过sysrecovery分区备份固件来实现功能）
;		anrecovery_key: 一键OTA 
;		注意：两个功能只能二选一
;----------------------------------------------------------------------------------
;[system]
;recovery_key             = port:PH16<0><default>
;anrecovery_key					 = port:PH16<0><default>

;----------------------------------------------------------------------------------
;	ir_used 		模块使能
;	ir_mode 		1: 一键进入OTA   2：一键恢复	其他值：无效
;	ir_rx			引脚配置
; 	ir_recovery_key 按键码，默认是power键值
; 	ir_addr_code	遥控器地址码
;----------------------------------------------------------------------------------
[ir_boot_para]
ir_used							= 0
ir_mode							= 1
ir_rx							= port:PB04<2><default><default><default>
ir_recovery_key					= 0x57					
ir_addr_code					= 0x9f00

;----------------------------------------------------------------------------------
; dvfs voltage-frequency table configuration
;
; max_freq: cpu maximum frequency, based on Hz, can not be more than 1008MHz
; min_freq: cpu minimum frequency, based on Hz, can not be less than 60MHz
;
; LV_count: count of LV_freq/LV_volt, must be < 16
;
; LV1: core vdd is 1.45v if cpu frequency is (912Mhz, 1008Mhz]
; LV2: core vdd is 1.40v if cpu frequency is (864Mhz, 912Mhz]
; LV3: core vdd is 1.30v if cpu frequency is (792Mhz, 864Mhz]
; LV4: core vdd is 1.25v if cpu frequency is (720Mhz, 792Mhz]
; LV5: core vdd is 1.20v if cpu frequency is (624Mhz, 720Mhz]
; LV6: core vdd is 1.15v if cpu frequency is (528Mhz, 624Mhz]
; LV7: core vdd is 1.10v if cpu frequency is (312Mhz, 528Mhz]
; LV8: core vdd is 1.05v if cpu frequency is ( 60Mhz, 312Mhz]
;
;----------------------------------------------------------------------------------
[dvfs_table]
max_freq = 1008000000
normal_freq = 1008000000
min_freq = 60000000

LV_count = 8

LV1_freq = 1008000000
LV1_volt = 1450

LV2_freq = 912000000
LV2_volt = 1400

LV3_freq = 864000000
LV3_volt = 1300

LV4_freq = 792000000
LV4_volt = 1250

LV5_freq = 720000000
LV5_volt = 1200

LV6_freq = 624000000
LV6_volt = 1150

LV7_freq = 528000000
LV7_volt = 1100

LV8_freq = 312000000
LV8_volt = 1050

[env_restore]
env_sub00 = "mac"
env_sub01 = "specialstr"

;----------------------------------------------------------------------------------
;boot display configuration
;output_type  (0:none; 1:lcd; 2:tv; 3:hdmi; 4:vga)
;output_mode  (used for tv/hdmi output, 0:480i 1:576i 2:480p 3:576p 4:720p50 5:720p60 6:1080i50 7:1080i60 8:1080p24 9:1080p50 10:1080p60 11:pal 14:ntsc)
;output_mode  (used for vga output, 0:1680*1050 1:1440*900 2:1360*768 3:1280*1024 4:1024*768 5:800*600 6:640*480 10:1920*1080 11:1280*720)
;auto_hpd     (auto detect hdmi/cvbs/ypbpr plug in)
;----------------------------------------------------------------------------------
[boot_disp]
output_type              = 3
output_mode              = 5
auto_hpd                 = 1
