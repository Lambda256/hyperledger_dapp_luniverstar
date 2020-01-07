/*
# Copyright Lambda256 Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
*/

const shim = require('fabric-shim');
const util = require('util');

var Chaincode = class {

  // Initialize the chaincode
  async Init(stub) {
    console.info('========= luniverstar Init =========');
    let ret = stub.getFunctionAndParameters();
    console.info(ret);
    let args = ret.params;
    // initialize only if 6 parameters passed.
    if (args.length != 6) {
      return shim.error('Incorrect number of arguments. Expecting 6');
    }

    let A = args[0];
    let B = args[2];
    let C = args[4];
    let Aval = args[1];
    let Bval = args[3];
    let Cval = args[5];

    if (typeof parseInt(Aval) !== 'number' || typeof parseInt(Bval) !== 'number' || typeof parseInt(Cval) !== 'number') {
      return shim.error('Expecting integer value for asset holding');
    }

    try {
      await stub.putState(A, Buffer.from(Aval));
      try {
        await stub.putState(B, Buffer.from(Bval));
        try {
          await stub.putState(C, Buffer.from(Cval));
          return shim.success();
        } catch (err) {
          return shim.error(err);
        }
      } catch (err) {
        return shim.error(err);
      }
    } catch (err) {
      return shim.error(err);
    }
  }

  async Invoke(stub) {
    let ret = stub.getFunctionAndParameters();
    console.info(ret);
    let method = this[ret.fcn];
    if (!method) {
      console.log('no method of name:' + ret.fcn + ' found');
      return shim.success();
    }
    try {
      let payload = await method(stub, ret.params);
      return shim.success(payload);
    } catch (err) {
      console.log(err);
      return shim.error(err);
    }
  }

  async invoke(stub, args) {
    if (args.length != 2) {
      throw new Error('Incorrect number of arguments. Expecting 2');
    }

    let A = args[0];
    if (!A) {
      throw new Error('asset holding must not be empty');
    }

    // Get the state from the ledger
    let Avalbytes = await stub.getState(A);
    if (!Avalbytes) {
      throw new Error('Failed to get state of asset holder A');
    }
    let Aval = parseInt(Avalbytes.toString());

    // Perform the execution
    let amount = parseInt(args[1]);
    if (typeof amount !== 'number') {
      throw new Error('Expecting integer value for amount to be transferred');
    }

    Aval = Aval + amount;
    console.info(util.format('Aval = %d\n', Aval));

    // Write the states back to the ledger
    await stub.putState(A, Buffer.from(Aval.toString()));

  }

  // Deletes an entity from state
  async delete(stub, args) {
    if (args.length != 1) {
      throw new Error('Incorrect number of arguments. Expecting 1');
    }

    let A = args[0];

    // Delete the key from the state in ledger
    await stub.deleteState(A);
  }

  // query callback representing the query of a chaincode
  async query(stub, args) {
    if (args.length != 1) {
      throw new Error('Incorrect number of arguments. Expecting name of the person to query')
    }

    let jsonResp = {};
    let A = args[0];

    // Get the state from the ledger
    let Avalbytes = await stub.getState(A);
    if (!Avalbytes) {
      jsonResp.error = 'Failed to get state for ' + A;
      throw new Error(JSON.stringify(jsonResp));
    }

    jsonResp.name = A;
    jsonResp.amount = Avalbytes.toString();
    console.info('Query Response:');
    console.info(jsonResp);
    return Avalbytes;
  }
};

shim.start(new Chaincode());
