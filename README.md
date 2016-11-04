[![Build Status](https://travis-ci.org/ysrotciv/backtesting.svg?branch=master)](https://travis-ci.org/ysrotciv/backtesting)

#数据准备
* 每日一组的数据（如行为数据，参照count_heat_yyyy-MM-dd）保存为zet，key为prefix_yyyy-MM-dd格式，value存股票代码字符串，score存对应的值。如果值为字符串形式，则使用hash来存。
* 只有一组的数据同上。

#协议
地址说明：
请求格式:http://wiki.smartdata-x.com/index.php/Back/kfreq/backtest
返回格式:http://wiki.smartdata-x.com/index.php/Back/kfrly/backtest
